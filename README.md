# Gestor Monchosos

[![CI](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/ci.yml/badge.svg)](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/ci.yml)
[![Seguridad](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/seguridad.yml/badge.svg)](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/seguridad.yml)

App Android para llevar el día a día de **Monchosos**, una hamburguesería en Xochitepec: tomar pedidos, cobrar y controlar el inventario. La idea viene de apps como Treinta, pero hecha a la medida del menú y la forma de trabajar del negocio.

## Qué hace

- Inicio de sesión con Google (solo cuentas autorizadas).
- Pedidos con el menú real: combos, tamaños, sabores, notas y entrega a domicilio.
- Cobro en efectivo, transferencia o tarjeta, con pagos divididos y cálculo de cambio.
- Inventario de insumos por kilo, litro o pieza, con aviso de stock bajo.
- Resumen del día: ventas, lo cobrado por método y lo que falta por cobrar.

Los datos viven en Supabase, así que los dos teléfonos del negocio ven lo mismo.

## Tecnologías

Kotlin · Jetpack Compose · Supabase (PostgreSQL + Auth) · GitHub Actions · Jenkins

## Cómo correrlo

1. Abre el proyecto en Android Studio.
2. En `local.properties` agrega:
   ```properties
   GOOGLE_WEB_CLIENT_ID=...
   SUPABASE_URL=...
   SUPABASE_PUBLISHABLE_KEY=...
   ```
3. Pide que agreguen tu correo a la tabla `usuarios_permitidos` en Supabase.
4. Ejecuta la app.

## Estructura

```
app/        App Android
supabase/   Migraciones, pruebas y configuración de la base de datos
ci/         Imagen Docker para compilar en Jenkins
infra/      Jenkins con Docker Compose
docs/       Guías
```

## Documentación

- [CONTEXTO.md](CONTEXTO.md): contexto del negocio, arquitectura y seguridad.
- [supabase/README.md](supabase/README.md): modelo de datos y cómo trabajar con la base.
- [docs/CI_SETUP.md](docs/CI_SETUP.md): cómo levantar Jenkins.

## Flujo de trabajo

Cada quien trabaja en su rama y abre un PR hacia `develop`. De `develop` se pasa a `main` cuando está estable. Los PRs tienen que pasar CI (pruebas, lint, compilación, base de datos y análisis de seguridad) antes de mezclarse.
