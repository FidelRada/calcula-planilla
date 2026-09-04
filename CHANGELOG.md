# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/);
versionado según [SemVer](https://semver.org/lang/es/).

## [1.2.0] — 2026-09-04

### Añadido
- `GET /api/planilla/parametros`: expone la configuración vigente de la instancia
  (salario mínimo, tasas y topes). Permite comprobar que BLUE y GREEN están
  configurados igual — si dos instancias liquidan distinto, es lo primero que hay
  que descartar.

## [1.1.0] — 2026-09-03

### Añadido
- Infraestructura de despliegue versionada en `infra/`: unidad systemd de plantilla
  `calcula-api@.service`, configuración de Nginx con `upstream` conmutable y `bootstrap.sh`.
- Scripts de despliegue en `scripts/`: `deploy.sh`, `rollback.sh`, `health-check.sh`,
  `smoke-test.sh`, `switch-traffic.sh` y `traffic-test.sh`.
- `README.md` con los catorce apartados de documentación exigidos.

## [1.0.0] — 2026-09-03

### Añadido
- `PlanillaService`: liquidación con escalones de antigüedad, tope de AFP y exención de RC-IVA.
- `PlanillaController` (`GET /api/planilla/liquidar`) e `InstanciaController` (`GET /api/instance`).
- Actuator para el health check del despliegue.
- Frontend estático en `web/`.
- Pipeline `maven.yml` con `Build`, `Test`, `Package` y `Release`.
- 34 pruebas con 100 % de cobertura de líneas y de ramas.
