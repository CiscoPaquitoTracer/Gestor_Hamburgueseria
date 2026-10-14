# Gestor_Hamburgueseria

App Android (Kotlin + Jetpack Compose) para administrar la hamburguesería **Monchosos**: inicio de sesión con Google e inventario de insumos (ingredientes por kg, desechables por pieza). Está inspirada en la app Treinta.

[![CI](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/ci.yml/badge.svg)](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/ci.yml)
[![Seguridad](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/seguridad.yml/badge.svg)](https://github.com/CiscoPaquitoTracer/Gestor_Hamburgueseria/actions/workflows/seguridad.yml)

👉 **Lee [CONTEXTO.md](CONTEXTO.md)** antes de empezar. Ahí está el contexto del negocio, la arquitectura, cómo configurar el login con Google, la seguridad y los pipelines.

## Inicio rápido

1. Abre el proyecto en Android Studio.
2. Agrega `GOOGLE_WEB_CLIENT_ID`, `SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY` a `local.properties` (ver CONTEXTO.md §4). Tu correo debe estar en `usuarios_permitidos` de Supabase.
3. Ejecuta la app.

## CI/CD

- **GitHub Actions:** pruebas, lint, compilación, base de datos y seguridad en cada push y PR (`.github/workflows/`).
- **Jenkins:** pipeline en Docker con release firmado en `main`. Instalación: **[docs/CI_SETUP.md](docs/CI_SETUP.md)**.
