# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/);
versionado según [SemVer](https://semver.org/lang/es/).

## [1.0.0] — 2026-09-03

### Añadido
- `PlanillaService`: liquidación con escalones de antigüedad, tope de AFP y exención de RC-IVA.
- `PlanillaController` (`GET /api/planilla/liquidar`) e `InstanciaController` (`GET /api/instance`).
- Actuator para el health check del despliegue.
- Frontend estático en `web/`.
- Pipeline `maven.yml` con `Build`, `Test`, `Package` y `Release`.
- 34 pruebas con 100 % de cobertura de líneas y de ramas.
