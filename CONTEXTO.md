# Contexto del proyecto: Monchosos Gestor

App Android para administrar una hamburguesería pequeña. Este documento explica **qué es, cómo está hecha y cómo se trabaja en ella**, para que cualquier persona (o asistente de IA) que llegue al proyecto entienda el contexto sin preguntar.

## 1. El negocio y quién usa la app

- **Negocio:** hamburguesería "Monchosos".
- **Usuarios:** una pareja que atiende el negocio. **No hay roles**: ambos ven y hacen lo mismo.
- **Inspiración:** la app [Treinta](https://www.treinta.co/), que sirve para llevar ventas, gastos, inventario y balance de negocios pequeños. La idea es llegar a algo parecido, pero hecho para una hamburguesería.
- **Diseño:** por ahora es **provisional y sencillo** (Material 3 con colores "ketchup y mostaza"). Otras personas lo van a rediseñar después, así que no conviene invertir en detalles visuales todavía.

## 2. Qué hace hoy (v0.1.0)

| Función | Estado |
|---|---|
| Inicio de sesión con el botón "Iniciar sesión con Google" | ✅ |
| Sesión recordada (no pide Google cada vez que se abre la app) | ✅ |
| Lista opcional de correos permitidos (solo la pareja puede entrar) | ✅ |
| Inventario de insumos: alta, edición, borrado, filtro por tipo | ✅ |
| Botones rápidos − / + para ajustar existencias | ✅ |
| Aviso de "por agotarse" según un stock mínimo | ✅ |
| Pantalla de inicio con resumen (número de insumos, valor del inventario, faltantes) | ✅ |
| Ventas, gastos, balance diario, recetas por hamburguesa | ⏳ pendiente |
| Sincronizar datos entre los dos teléfonos | ⏳ pendiente (ver §7) |

### Modelo del inventario

Cada **insumo** tiene:

| Campo | Ejemplo | Notas |
|---|---|---|
| nombre | Pan, Lechuga, Jitomate, Caja chica | |
| categoría | `INGREDIENTE`, `DESECHABLE`, `OTRO` | |
| unidad | `KILOGRAMO` o `PIEZA` | Ingredientes sugieren **kg** y desechables **pieza**, pero se puede cambiar (por ejemplo, el pan por pieza). |
| cantidad | 2.5 kg, 100 pz | Las piezas solo aceptan números enteros. |
| stockMínimo | 1 kg | Opcional. Si la cantidad llega a este valor o baja de él, se marca como "por agotarse". |
| costoUnitario | $35 / kg | Opcional. Sirve para calcular el valor del inventario. |

Los botones − / + mueven **1 pieza** o **0.5 kg**. Para ajustes exactos, se toca el insumo y se edita.

## 3. Tecnología

- **Kotlin** + **Jetpack Compose** (Material 3), un solo módulo `:app`.
- **minSdk 24**, target/compile **37**, AGP 9, Gradle 9.
- **Supabase** (PostgreSQL en la nube) con [supabase-kt](https://github.com/supabase-community/supabase-kt): Auth + Postgrest sobre Ktor/OkHttp, con kotlinx-serialization. **Necesita internet.**
- **Credential Manager** + `googleid` para el inicio de sesión con Google; el token se entrega a Supabase Auth.
- **Navigation Compose** para la barra inferior (Inicio / Pedidos / Inventario).
- Versiones centralizadas en [`gradle/libs.versions.toml`](gradle/libs.versions.toml). **No escribas versiones directamente en `build.gradle.kts`.**

### Estructura

```
app/src/main/java/com/example/hamgurgueseria_monchosos/
├── HamburgueseriaApp.kt        # Application: crea el cliente de Supabase, la sesión y los repositorios
├── MainActivity.kt             # Cargando → Login → AppPrincipal según la sesión de Supabase
├── auth/
│   ├── GoogleAuthClient.kt     # Google (Credential Manager, con nonce) → Supabase Auth; revisa usuarios_permitidos
│   └── Sesion.kt               # Traduce la sesión de Supabase a EstadoSesion/Usuario
├── data/
│   ├── remoto/Supabase.kt      # Cliente, mensajes de error, conversiones de fechas y dinero
│   ├── menu/                   # Menú (catálogo de la app) y armarRenglon (valida combos, sabores, variantes)
│   ├── pedidos/                # Modelos y PedidosRepository (ventas, pagos, guardar_venta)
│   ├── Insumo.kt               # Modelo + enums Unidad/Categoria + formateo
│   └── InventarioRepository.kt # Insumos (guardar_insumo, ajustar_existencia)
└── ui/
    ├── login/                  # Pantalla de inicio de sesión
    ├── inicio/                 # Resumen del día (ventas, cobrado por método, por cobrar) + inventario
    ├── pedidos/                # Lista, editor (menú → resumen), detalle, cobrar
    ├── inventario/             # Lista, diálogo de alta/edición, validación
    ├── comun/                  # Aviso de error y actualización periódica
    ├── navegacion/             # Scaffold + barra inferior + NavHost
    └── theme/                  # Colores y tipografía (provisionales)
```

### Base de datos en la nube: Supabase

Los datos viven en **Supabase** (PostgreSQL 17), así que los dos teléfonos comparten la misma información. Las pantallas se actualizan solas cada 20–30 s y después de cada cambio. La app se conecta directo con la llave pública, y la seguridad está en la base de datos: Row Level Security, donde solo entran los correos de `usuarios_permitidos`. El esquema se versiona como migraciones en [`supabase/`](supabase/README.md); ahí están las instrucciones, el modelo de datos, las reglas y los reportes.

Las reglas de negocio importantes viven en la base de datos, no en el teléfono: precios congelados, estado pagado/por cobrar, descuento de inventario por receta, y guardar un pedido completo en una sola transacción (`guardar_venta`).

**Para agregar una sección nueva** (por ejemplo, Gastos): crea `ui/gastos/`, agrégala al enum `Seccion` y al `NavHost` en [`AppPrincipal.kt`](app/src/main/java/com/example/hamgurgueseria_monchosos/ui/navegacion/AppPrincipal.kt), y crea un repositorio en `data/` que use el cliente de Supabase. Si necesitas tablas o funciones nuevas, crea una migración en `supabase/migrations/`.

## 4. Configurar el inicio de sesión con Google

Sin esta configuración la app compila, pero el botón de Google muestra un aviso.

1. En [Google Cloud Console](https://console.cloud.google.com/), crea un proyecto y configura la **pantalla de consentimiento de OAuth** (tipo externo; agrega los correos de la pareja como usuarios de prueba).
2. En **Credenciales**, crea un ID de cliente OAuth de tipo **Android**:
   - Paquete: `com.example.hamgurgueseria_monchosos`
   - SHA-1: sale de `./gradlew signingReport` (variante `debug`). Cuando exista una llave de release, crea otro cliente con el SHA-1 de esa llave.
3. Crea otro ID de cliente de tipo **Aplicación web**. Ese ID es el que usa la app.
4. Agrega a `local.properties` (este archivo **no** se sube a git):
   ```properties
   GOOGLE_WEB_CLIENT_ID=1234567890-xxxxxxxx.apps.googleusercontent.com
   SUPABASE_URL=https://<proyecto>.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...   # NUNCA la service_role / secret key
   ```
5. En Supabase: **Authentication → Sign In / Providers → Google** activado, con el ID web en "Client IDs".
6. Da acceso agregando los correos en la tabla **`usuarios_permitidos`** (ver [supabase/README.md](supabase/README.md)).
7. Sincroniza Gradle y vuelve a instalar la app.

> En CI se pueden pasar los mismos valores como variables de entorno o secretos.

El token de Google se entrega a **Supabase Auth**, que lo verifica en el servidor. Se usa un *nonce* para que un token interceptado no se pueda reutilizar. Si la cuenta no está en `usuarios_permitidos`, la app cierra la sesión y avisa.

## 5. Seguridad

| Medida | Dónde |
|---|---|
| **Cifrado en las conexiones:** solo HTTPS/TLS. El tráfico sin cifrar (`http://`) está bloqueado y solo se confía en los certificados del sistema, no en los que instale el usuario. | [`res/xml/network_security_config.xml`](app/src/main/res/xml/network_security_config.xml), manifiesto |
| **Row Level Security** en todas las tablas: sin sesión no se ve nada; con sesión, solo los correos de `usuarios_permitidos` | [`supabase/migrations/…02_seguridad.sql`](supabase/migrations) |
| Funciones internas fuera de la API pública (esquema `interno`) | migración de seguridad |
| Login de Google verificado por Supabase Auth, con nonce | `GoogleAuthClient.kt` |
| Sin respaldos de Android (`allowBackup=false`): los datos están en la nube y la sesión no se copia a otro teléfono | manifiesto |
| R8 (ofuscación y minificación) en release | `app/build.gradle.kts` |
| Los secretos viven en `local.properties` o en secretos de CI, nunca en el código | `.gitignore` |

### Pentesting automático (pipeline de [Seguridad](.github/workflows/seguridad.yml))

- **CodeQL:** análisis estático del código Kotlin (inyecciones, malas prácticas, etc.).
- **mobsfscan (MobSF):** reglas específicas de Android (manifiesto, almacenamiento inseguro, criptografía débil, etc.).
- **Gitleaks:** detecta tokens o contraseñas subidos por error.
- **Dependency review:** en cada PR, bloquea librerías nuevas con vulnerabilidades altas.
- **Grafo de dependencias + Dependabot alerts:** avisa si alguna librería que ya usamos resulta vulnerable.

Los resultados se ven en la pestaña **Security → Code scanning** de GitHub. Para revisar un hallazgo:
1. Si es real, se corrige en un PR.
2. Si es un falso positivo, se descarta en GitHub con una justificación.

Esto **no sustituye** un pentest manual. Antes de publicar en Play Store conviene pasar el APK de release por [MobSF](https://github.com/MobSF/Mobile-Security-Framework-MobSF) (análisis dinámico) y revisar la lista [OWASP MASVS](https://mas.owasp.org/MASVS/).

## 6. Pipelines (GitHub Actions) y versionamiento

| Archivo | Cuándo corre | Qué hace |
|---|---|---|
| [`ci.yml`](.github/workflows/ci.yml) | push y PR a `main` | Pruebas unitarias, lint, compila debug y release, y sube los reportes y el APK debug como artefactos |
| [`seguridad.yml`](.github/workflows/seguridad.yml) | push y PR a `main`, y cada lunes | CodeQL, mobsfscan, Gitleaks, dependency review y grafo de dependencias |
| [`dependabot.yml`](.github/dependabot.yml) | cada lunes | Abre PRs para actualizar librerías de Gradle y Actions (las de AndroidX y las de Kotlin/AGP van agrupadas) |

**Versionamiento:**
- **Dependencias:** todas están en `libs.versions.toml` y Dependabot propone las actualizaciones. Un PR de Dependabot se mezcla solo si CI y Seguridad pasan en verde.
- **App:** se usa [SemVer](https://semver.org/lang/es/) en `versionName` (`MAYOR.MENOR.PARCHE`) dentro de `app/build.gradle.kts`, y `versionCode` sube en 1 con cada versión que se instala. Cada versión publicada se marca con un tag `vX.Y.Z`.
- **Ramas:**
  ```
  main  ◄── PR ──  develop  ◄── PR ──  francisco (y otras ramas personales)
  ```
  `main` es lo estable (lo que se instala en los teléfonos) y `develop` es donde se integra el trabajo. Cada persona trabaja en su rama, que sale de `develop`, y entra a `develop` por PR. Cuando `develop` está estable, se abre un PR de `develop` a `main`. Ambas ramas están protegidas: solo aceptan cambios por PR y con CI en verde. Los PRs de Dependabot llegan a `develop`.

## 7. Siguientes pasos sugeridos

1. **Ventas** (como Treinta): registrar cada venta (hamburguesas, combos, bebidas) con total y método de pago.
2. **Recetas:** cada hamburguesa descuenta sus ingredientes del inventario al venderse (por ejemplo, 1 pan, 0.15 kg de carne y 0.02 kg de lechuga).
3. **Gastos y balance diario/semanal.**
4. **Sincronización entre los dos teléfonos:** hoy cada teléfono tiene su propia base de datos. La opción más directa es Firebase (Auth con Google + Firestore). El punto de conexión es `InventarioRepository`.
5. **Firma de release y publicación:** crear un keystore (guardado como secreto, nunca en git) y un workflow que genere el AAB al crear un tag `v*`.
6. **Diseño final**, a cargo del equipo de diseño.

## 8. Comandos útiles

```bash
./gradlew testDebugUnitTest   # pruebas unitarias
./gradlew lintDebug           # lint (reporte en app/build/reports/)
./gradlew assembleDebug       # APK debug en app/build/outputs/apk/debug/
./gradlew signingReport       # SHA-1 para configurar Google
```
