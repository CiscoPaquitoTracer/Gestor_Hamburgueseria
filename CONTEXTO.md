# Contexto del proyecto: Monchosos

App Android para administrar una hamburguesería pequeña. Este documento explica **qué es, cómo está hecha y cómo se trabaja en ella**, para que cualquier persona (o asistente de IA) que llegue al proyecto entienda el contexto sin preguntar.

> Datos del negocio (ventas reales, costos, correos) **no** van en este repo: es público. Los Excel del negocio (`*.xlsx`) y el SQL generado a partir de ellos están en `.gitignore`.

## 1. El negocio y quién usa la app

- **Negocio:** hamburguesería "Monchosos", en Xochitepec, Morelos.
- **Abre de viernes a domingo.**
- **Usuarios:** una pareja que atiende el negocio. **No hay roles**: ambos ven y hacen lo mismo, cada quien con su teléfono.
- **Inspiración:** la app [Treinta](https://www.treinta.co/) (ventas, gastos, inventario y balance de negocios pequeños).
- **Objetivo actual:** que dejen de usar Excel (antes llevaban las comandas y las compras en hojas de cálculo) y usen solo la app.
- **Diseño:** provisional y sencillo (Material 3). Otras personas lo van a rediseñar; no conviene invertir en detalles visuales.
- **Ícono:** hamburguesa vaquera sobre fondo café `#571401` (`res/mipmap-*`, ícono adaptativo).

## 2. Qué hace hoy

Barra inferior: **Inicio · Pedidos · Resumen · Inventario · Menú**.

| Pantalla | Qué hace |
|---|---|
| Login | "Iniciar sesión con Google" → Supabase Auth. Solo entran los correos de `usuarios_permitidos`. La sesión se recuerda. |
| Inicio | Ventas, pedidos y cobrado de **hoy** (se reinicia a medianoche); tarjeta de **pronóstico** (próximo fin de semana o festivo, productos esperados y qué comprar); inventario por agotarse. |
| Pedidos | Lista con filtros **Por cobrar** (por defecto, solo los no pagados) · Hoy · Todos (últimos 300). Editor con el menú real (combos, tamaños, sabores, notas, entrega a domicilio), detalle, cobro con pagos divididos (efectivo, transferencia, tarjeta) y cálculo de cambio. |
| Resumen | Resumen **del mes** con ◀ ▶: vendido y comparación con el mes anterior, pedidos, promedio por pedido, días abiertos, cobrado por método, por cobrar, descuentos, **ganancia estimada** (ventas − costo de lo vendido − gastos), 5 más vendidos y promedio por día de la semana. Incluye los meses importados del Excel (estimados con precios actuales). |
| Inventario | Insumos agrupados por sección (carnes, lácteos, panes, verduras, aderezos y salsas, abarrotes, bebidas, desechables, otros), por kg, litro o pieza; alta, edición, baja; botones − / +; aviso de stock mínimo. Cada venta **descuenta los insumos según la receta** (cancelar un pedido los devuelve). |
| Menú | Cambiar **precios** y marcar productos/tamaños como **no disponibles** (dejan de aparecer al tomar pedidos). Los pedidos ya hechos conservan su precio; cada cambio queda en `historial_precios`. |
| Notificación | Diario a las 10:00 (WorkManager): si mañana o pasado empieza un fin de semana o festivo **y** se suele vender más (≥ 15 %) o faltan insumos, avisa qué comprar. |

### Pronóstico (cómo estima)

Funciones `pronostico_dias` y `pronostico_insumos` (migración 008). Se mide en **productos vendidos** (no pedidos) para poder sumar el historial del Excel:
- **Festivo** → como los festivos anteriores (último año). Calendario `dias_festivos` 2025–2030 (oficiales, Semana Santa calculada y fechas comerciales); se pueden agregar fechas propias.
- **Otro día** → promedio de los **últimos 8 días iguales** (ej. últimos 8 sábados) que tuvieron ventas.
- Con poco historial (< 8 días) se usa el promedio general; con más, un día de la semana sin ventas se toma como **cerrado**.
- Insumos esperados = recetas × productos esperados; `por_comprar` = lo que falta para cubrir esos días y no bajar del stock mínimo.

### Historial anterior a la app (Excel)

- Tabla **`historial_ventas`** (día, producto, variante, cantidad; los combos también guardan su contenido con `componente = true`). Solo la usan el pronóstico y el resumen; **no** entra al corte de caja. Si un día existe en la app y en el historial, manda la app.
- [`supabase/importar_comandas.py`](supabase/importar_comandas.py) convierte `Comandas.xlsx` en SQL. El Excel no trae fechas: los bloques de cada hoja se asignan en orden a los días que abren (`--dias vie,sab,dom`). Correr con Python + openpyxl (en esta laptop, vía Docker `python:3.12-alpine`) y aplicar con `npx supabase@2.118.0 db query --linked -f comandas.sql`. Se puede volver a correr: reemplaza lo importado.
- Cuando lleguen más Excels (organizados por día, con fecha), adaptar el script.

## 3. Tecnología

- **Kotlin** + **Jetpack Compose** (Material 3), un solo módulo `:app`. **minSdk 24** (sin `java.time`: fechas como texto ISO o `Calendar`), target/compile **37**, AGP 9, Gradle 9, JDK 21 en GitHub Actions y 25 en la imagen de Jenkins.
- **Supabase** (PostgreSQL 17 en la nube, plan Free) con [supabase-kt](https://github.com/supabase-community/supabase-kt): Auth + Postgrest sobre Ktor/OkHttp y kotlinx-serialization. **Necesita internet.** Proyecto pausado si pasa 7 días sin uso (se reactiva desde el panel).
- **Credential Manager** + `googleid` (con nonce) para Google.
- **WorkManager** para el aviso diario de reabasto.
- Versiones en [`gradle/libs.versions.toml`](gradle/libs.versions.toml). **No escribas versiones en `build.gradle.kts`.**

### Estructura

```
app/src/main/java/com/example/hamgurgueseria_monchosos/
├── HamburgueseriaApp.kt        # Application: cliente de Supabase, sesión, repositorios; programa el aviso diario
├── MainActivity.kt             # Cargando → Login → AppPrincipal según la sesión
├── auth/                       # GoogleAuthClient (Credential Manager → Supabase), Sesion
├── avisos/AvisoReabasto.kt     # Worker diario + notificación de reabasto
├── data/
│   ├── remoto/Supabase.kt      # Cliente, mensajes de error, fechas y dinero (centavos ↔ pesos)
│   ├── menu/                   # Menu.kt (estructura del menú + precios de respaldo), MenuRepository (precios/disponibilidad), armarRenglon
│   ├── pedidos/                # Modelos y PedidosRepository (ventas, pagos, guardar_venta)
│   ├── pronostico/             # Modelos, proximoEvento/mensajeDeAviso (lógica pura), PronosticoRepository
│   ├── resumen/                # ResumenRepository (función resumen_mes)
│   └── Insumo.kt, InventarioRepository.kt
└── ui/
    ├── login/  inicio/ (incl. PronosticoCard)  pedidos/  resumen/  inventario/  menu/
    ├── comun/                  # Aviso de error y actualización periódica
    ├── navegacion/             # Scaffold + barra inferior + NavHost + permiso de notificaciones
    └── theme/
```

**Menú:** la *estructura* (combos, descripciones, categorías) está en `Menu.kt`; los *precios y la disponibilidad* vienen de Supabase y se aplican con `Menu.actualizar`. Productos y combos nuevos todavía requieren cambiar `Menu.kt` (y la tabla `productos`).

### Base de datos (Supabase)

Esquema versionado como migraciones en [`supabase/migrations/`](supabase/README.md) (001 esquema … 007 endurecer acceso, **008 pronóstico**, **009 resumen**, **010 categorías de insumo**). Pruebas en [`supabase/tests/pruebas.sql`](supabase/tests/pruebas.sql) (12 grupos, incluyen RLS y suplantación), que corren en CI.

Las reglas de negocio viven en la base de datos: precios congelados por venta, estado pagado/por cobrar según pagos, descuento de inventario por receta (triggers), pedido completo en una transacción (`guardar_venta`), pronóstico y resumen.

Tablas que existen pero la app aún **no** usa: `gastos`, `compras`/`compra_detalle` (actualizan el costo del insumo), `turnos` (corte de caja), `prestamos`/`abonos`, `activos`, `proveedores`.

**Para agregar una sección:** crea `ui/<seccion>/`, súmala al enum `Seccion` y al `NavHost` en [`AppPrincipal.kt`](app/src/main/java/com/example/hamgurgueseria_monchosos/ui/navegacion/AppPrincipal.kt), un repositorio en `data/`, y si hace falta una migración nueva (con RLS `equipo_todo` en tablas nuevas, `REVOKE … FROM anon`, funciones internas en el esquema `interno`, vistas con `security_invoker`) y su grupo en `pruebas.sql`.

## 4. Configurar el inicio de sesión con Google

1. En [Google Cloud Console](https://console.cloud.google.com/): pantalla de consentimiento OAuth y, en **Credenciales**, dos clientes **Android** con el paquete `com.monchosos.app` (uno con el SHA-1 de debug de `./gradlew signingReport` y otro con el de la llave de release) y un cliente **Aplicación web** (ese ID usa la app).
2. En `local.properties` (no se sube a git):
   ```properties
   GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com
   SUPABASE_URL=https://<proyecto>.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...   # NUNCA la service_role / secret key
   ```
3. En Supabase: **Authentication → Providers → Google** con el ID web, y los correos en **`usuarios_permitidos`** (desde el panel; nunca en migraciones, el repo es público).

## 5. Seguridad

| Medida | Dónde |
|---|---|
| Solo HTTPS; sin certificados de usuario | `res/xml/network_security_config.xml` |
| **RLS** en todas las tablas: solo Google verificado + correo en `usuarios_permitidos` (`es_usuario_permitido()`) | migraciones 002 y 007 |
| Funciones internas en el esquema `interno` (fuera de `/rpc`) | migración 002 |
| `allowBackup=false`, `taskAffinity=""` (StrandHogg), `filterTouchesWhenObscured` (tapjacking) | manifiesto, `MainActivity` |
| R8 en release; secretos solo en `local.properties` o secretos de CI | `app/build.gradle.kts`, `.gitignore` |

Pipeline de [Seguridad](.github/workflows/seguridad.yml): CodeQL, mobsfscan, Gitleaks, dependency review y grafo de dependencias. Hallazgos en **Security → Code scanning**: si es real se corrige en un PR; si es falso positivo se descarta con justificación.

## 6. CI/CD y ramas

| Dónde | Qué |
|---|---|
| [`ci.yml`](.github/workflows/ci.yml) | Pruebas, lint, APK debug y release; base de datos (migraciones, `db lint`, `pruebas.sql`) |
| [`seguridad.yml`](.github/workflows/seguridad.yml) | Análisis de seguridad (ver §5) |
| [`imagenes-docker.yml`](.github/workflows/imagenes-docker.yml) | Construye `ci/Dockerfile` e `infra/jenkins` y compila la app dentro cuando cambian |
| Dependabot | PRs semanales a `develop` (Gradle, Actions, imágenes Docker) |
| **Jenkins** ([`Jenkinsfile`](Jenkinsfile), [`docs/CI_SETUP.md`](docs/CI_SETUP.md)) | Corre en Docker en la laptop (`http://localhost:8081`; el 8080 lo ocupa XAMPP). Pipeline multirrama `monchosos` ya incluido (`infra/jenkins/jobs`), escanea GitHub cada 5 min, sin forks. Lint → Unit Tests → APK debug; en `main` además **Build Release** (APK/AAB firmados) y **Publicar en GitHub** (release `v1.0.N` con el APK). Sin la llave de release esas dos etapas se omiten. |

Ramas: `francisco` (trabajo) → PR → `develop` → PR → `main`. `main` y `develop` solo por PR, sin borrar ni force-push. Commits con prefijo `[ADD]` / `[FIX]` / `[DOC]`, sin coautor.

## 7. Pendientes

1. **Costos reales de insumos** (de `Precio.xlsx`) y **recetas reales**: hoy las recetas son de ejemplo y muchos insumos no tienen costo, así que el pronóstico de compras y la ganancia del Resumen son aproximados.
2. **Captura de gastos y compras en la app** (las tablas ya existen) para que el Resumen muestre la ganancia completa y dejen el Excel.
3. Más **Excels históricos** organizados por día → importar con el script.
4. **Llave de release** + SHA-1 en Google Cloud + credenciales en Jenkins → APK firmado y publicado en GitHub Releases (el pipeline ya está listo).
5. Exigir el check de Jenkins en las reglas de `main`/`develop`.
6. Productos/combos nuevos desde la app (hoy viven en `Menu.kt`).
7. Diseño final, a cargo del equipo de diseño.

## 8. Comandos útiles

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug   # usar JAVA_HOME = JBR de Android Studio
npx supabase@2.118.0 db start                         # Postgres local con las migraciones
docker exec -i supabase_db_Gestor_Hamburgueseria psql -U postgres -v ON_ERROR_STOP=1 -f - < supabase/tests/pruebas.sql
npx supabase@2.118.0 db lint --local --level warning --fail-on warning
npx supabase@2.118.0 db push --linked                  # aplicar migraciones a producción (con permiso)
```
