# Calcula — plataforma de liquidación de planilla

Proyecto Final del **Módulo 4 · CI/CD** del Diplomado DevOps.

**Integrantes:** Bautista Condori Anderzon · Grichukin Mendez Richard ·
Rada Rojas Andres Fidel · Vargas Rios Bebi

---

## 1. Descripción del proyecto

API REST que liquida una planilla —entra un salario bruto y los años de antigüedad, sale el
desglose y el líquido pagable— acompañada de un frontend estático. El objetivo del proyecto no es
la aplicación sino **el proceso**: llevar un cambio desde un commit hasta producción de forma
automatizada, repetible, verificable, trazable y documentada.

La aplicación se eligió por una propiedad concreta: **es pura y sin estado**. Eso permite que dos
instancias convivan sin conflicto, que es la precondición del despliegue Blue-Green, y evita el
problema de las migraciones retrocompatibles que haría el rollback mucho más difícil.

## 2. Arquitectura

```
Desarrollador → GitHub → Pull Request → GitHub Actions
                                              │
                              Build · Test · Coverage · Package
                                              │
                                         tag vX.Y.Z
                                              │
                                      GitHub Release (.jar + web.tgz + SHA256SUMS)
                                              │
                                          deploy.sh
                                              │
                                    appserver (Ubuntu 24.10)
                                              │
                                          Nginx :80
                                       ┌──────┴──────┐
                                  BLUE :8080    GREEN :8081
                                       └──────┬──────┘
                                  health · smoke · traffic-test
                                       ┌──────┴──────┐
                                     PASS          FAIL
                                       │              │
                                   promoción      rollback
```

El único objeto que cruza todas las etapas es el `.jar`, y **nunca se reconstruye**.

## 3. Tecnologías

Java 21 · Spring Boot 4.1.0 · Maven · JUnit 5 · JaCoCo 0.8.13 · GitHub Actions ·
GitHub Releases · Nginx · systemd · Bash · Ubuntu Server 24.10 · VirtualBox · SSH

## 4. Estrategia de branching

**Trunk-based development.**

| Rama | Propósito | Vida |
|---|---|---|
| `main` | Única de larga vida. Siempre desplegable; todo tag sale de aquí. Protegida | permanente |
| `feature/*` | Una por unidad de trabajo | < 2 días |
| `hotfix/*` | Corrección urgente sobre producción | horas |

**Reglas para crear ramas:** siempre desde `main` actualizada; nombre en kebab-case que describa
el trabajo, no a la persona; una rama = una unidad entregable.

**Reglas para hacer merge:** solo por Pull Request; los tres checks (`Build`, `Test`, `Package`)
en verde; rama actualizada con `main`; **squash-merge**, para que un cambio sea un commit y un
SHA; borrar la rama al fusionar.

**Protección de `main`:** `enforce_admins`, checks requeridos con `strict`, PR obligatorio, sin
force-push ni borrado. *El repositorio es público porque GitHub bloquea la protección de rama en
repos privados del plan gratuito.*

**Relación con el pipeline:**

| Evento | Jobs |
|---|---|
| `pull_request` → `main` | Build · Test · Package |
| `push` a `main` | Build · Test · Package |
| `push` de tag `v*` | Build · Test · Package · **Release** |
| `push` a `feature/*` | ninguno |

## 5. Estrategia de tagging

**SemVer** `MAJOR.MINOR.PATCH`, con el tag prefijado (`v1.0.0`).

- **MAJOR** — cambio incompatible del contrato de la API.
- **MINOR** — funcionalidad nueva retrocompatible.
- **PATCH** — corrección sin cambio de contrato.

**Cuándo se crea:** solo cuando una versión de `main` se decide publicar y desplegar. Nunca desde
una rama `feature`, nunca sobre un commit sin CI verde.

**Relación tag ↔ artifact ↔ Release: 1:1:1 e inmutable.** El tag dispara un run; ese run produce
un jar; ese jar se adjunta a una Release con el nombre del tag.

## 6. Pipeline CI/CD

`.github/workflows/maven.yml`, cuatro jobs encadenados por `needs`:

| Job | Comando | Bloquea |
|---|---|---|
| `Build` | `mvn -B clean compile --file pom.xml` | sí |
| `Test` | `mvn -B test` → `jacoco:report` → `jacoco:check` | sí |
| `Package` | `mvn -B package -DskipTests` + `upload-artifact` | sí |
| `Release` | `download-artifact` + `action-gh-release` | solo en tags |

Los comandos usan `mvn --file pom.xml`: el enunciado prohíbe depender de `./mvnw` en Actions. El
wrapper se conserva para el trabajo local.

**Cobertura actual: 100 % de líneas y 100 % de ramas**, con 48 pruebas. El umbral del Quality Gate
vive parametrizado en `jacoco.cobertura.minima` (0.80) y aplica a líneas **y a ramas**.
JaCoCo cuenta **veinte ramas** en `PlanillaService`: las dos salidas de cada punto de decisión —la
comparación del bucle de escalones, las dos validaciones de entrada, el indicador de tope de AFP,
el de exención de RC-IVA y los cuatro del aguinaldo—, no una por escalón. Recorrer seis escalones
no crea seis ramas, crea un `if` evaluado seis veces.

El ejemplo que justifica medir ramas está en el tope de AFP. La expresión `ganado > topeAfp()` se
ejecuta en **toda** liquidación, así que su línea aparece cubierta siempre; pero si ningún caso de
prueba usa un salario por encima del tope, esa decisión solo se evalúa en una dirección y **la
cobertura de ramas baja mientras la de líneas sigue al 100 %**. Por eso existe la prueba que
liquida 200.000 Bs.

**Cuando una prueba falla:** `Test` sale con código ≠ 0 → `Package` no llega a ejecutarse por
`needs` → falta un check requerido → **el PR no se puede fusionar**.

## 7. Generación del artifact

`Package` construye **una sola vez** y sube `calcula-api-<version>.jar` y `calcula-web.tgz` como
artefactos del run. Ningún otro job vuelve a compilar.

## 8. GitHub Release

`Release` se dispara solo con `startsWith(github.ref, 'refs/tags/v')` y recupera el paquete con
`download-artifact` **del mismo run**, así que el jar publicado es literalmente el que produjo
`Package`. Publica además un `SHA256SUMS` que hace la trazabilidad comprobable con un comando.

## 9. Configuración de la infraestructura local

Dos VM Ubuntu Server 24.10 en VirtualBox, con adaptador puente:

| | `cicd` | `appserver` |
|---|---|---|
| IP | 192.168.100.170 | 192.168.100.171 |
| Rol | orquestador del despliegue | destino: Nginx + BLUE + GREEN |

Acceso por llave SSH, sin contraseña. `infra/bootstrap.sh` deja `appserver` listo desde cero:
instala Nginx, crea `/opt/calcula`, instala la unidad `calcula-api@.service` y los entornos.

**Una unidad systemd de plantilla sirve para las dos instancias.** `%i` vale `blue` o `green`, y
el `EnvironmentFile` correspondiente da `APP_INSTANCE` y `SERVER_PORT`. Por eso **el mismo `.jar`
sirve para ambas**: la identidad viene de la configuración, nunca de una recompilación.

## 10. Estrategia de deployment

**Blue-Green.** Justificación por los factores que exige el enunciado:

- **Características de la aplicación** — servicio HTTP sin estado, arranca en segundos: dos
  instancias coexisten sin conflicto.
- **Infraestructura disponible** — una VM con 2 GB sostiene dos JVM. Canary exigiría métricas por
  versión que no tenemos.
- **Complejidad** — el switch es reescribir un `upstream` y recargar Nginx.
- **Riesgo** — la versión nueva se valida *antes* de recibir tráfico real.
- **Rollback** — la ventaja decisiva: la versión anterior sigue encendida.
- **Verificación** — `/api/instance` hace observable qué versión atendió cada petición.

**Coste declarado:** el doble de memoria durante el despliegue.

## 11. Ejecución de los scripts

```bash
./scripts/deploy.sh 1.1.0     # despliega al color inactivo y conmuta si verifica
./scripts/rollback.sh         # vuelve al color anterior y lo comprueba

# en appserver
/opt/calcula/scripts/health-check.sh 8081
/opt/calcula/scripts/smoke-test.sh 8081
/opt/calcula/scripts/switch-traffic.sh green
/opt/calcula/scripts/traffic-test.sh 20
```

`deploy.sh` cubre los ocho pasos exigidos: identifica la versión y el color destino, descarga el
artefacto de la Release, **verifica el `sha256sum`**, prepara el ambiente, detiene la versión
anterior *del color destino*, instala, arranca, verifica y conmuta.

## 12. Health checks

`health-check.sh <puerto>` consulta `/actuator/health` con reintentos y timeout. Se ejecuta contra
el **puerto directo del color nuevo**, no por el `:80`: por ahí todavía responde la versión vieja.

## 13. E2E tests

`smoke-test.sh <puerto>` comprueba tres liquidaciones con valores exactos, que una entrada
inválida devuelva 400 y que `/api/instance` exponga la versión. **Los valores esperados son los
mismos que fija `PlanillaServiceTest`**, así que la prueba unitaria y la de humo comprueban lo
mismo desde los dos lados del despliegue.

**Condición para continuar:** health y smoke deben pasar *en secuencia*. Si cualquiera falla,
`deploy.sh` detiene la instancia nueva y sale con código ≠ 0 **sin conmutar el tráfico**.

## 14. Procedimiento de rollback

- **Cómo se detecta el fallo** — health o smoke fallan antes del switch; o el tráfico se degrada
  después.
- **Cómo se aísla la versión defectuosa** — *primero se aísla, después se apaga*: se saca del
  `upstream` y solo entonces se detiene, para no cortar peticiones en vuelo.
- **Cómo se recupera la anterior** — no hay que recuperarla: **nunca se apagó**.
- **Cómo se restablece el tráfico** — `switch-traffic.sh <color>` reescribe el `upstream`, valida
  con `nginx -t` y aplica con `nginx -s reload` (recarga, no reinicio).
- **Cómo se verifica** — `traffic-test.sh 20` debe devolver 20/20 del color anterior.

---

## Nota sobre las cifras de planilla

Los porcentajes y tramos de `application.properties` son **valores de ejemplo parametrizados, no
una fuente legal**. Lo que las pruebas fijan es la lógica —dónde cortan los escalones, que el tope
se aplique, que la exención funcione— no la exactitud normativa. Para uso real hay que verificarlos
contra la norma vigente.
