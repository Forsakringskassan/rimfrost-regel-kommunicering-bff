# rimfrost-regel-kommunicering-bff

Backend-for-frontend for the kommunicering micro-frontend. Proxies the
[rimfrost-regel-kommunicering](https://github.com/Forsakringskassan/rimfrost-regel-kommunicering) rule service,
using the models generated from
[rimfrost-regel-kommunicering-openapi](https://github.com/Forsakringskassan/rimfrost-regel-kommunicering-openapi).

Built from [rimfrost-template-micro-fe-bff](https://github.com/Forsakringskassan/rimfrost-template-micro-fe-bff)
on Quarkus and `rimfrost-framework-bff`, which provides error mapping, logging context and the upstream health check.

## Running locally

```shell script
./mvnw compile quarkus:dev
```

The application listens on port **9009**. Dev UI: <http://localhost:9009/q/dev/>.

Full build, as in CI but without the container image:

```shell script
./mvnw verify -Dquarkus.container-image.build=false
```

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080/regel/kommunicering` | Full base URL of the kommunicering backend, including the rule path. No trailing slash. |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:3030` | Comma-separated list of allowed CORS origins. |
| `CORS_ALLOW_CREDENTIALS` | `false` | Set to `true` if the frontend sends cookies cross-origin. |
| `KAFKA_BOOTSTRAP_SERVERS` | | Kafka address for the ILogg `ilogg-event` channel. |

## REST endpoints

| Method | Path | Backend call | Description |
|---|---|---|---|
| `POST` | `/api/task` | `GET /{handlaggningId}` | Fetches kund and ersättningar. Body: `{"handlaggningId": "<uuid>"}`. Returns the backend's `GetDataResponse`. |
| `PATCH` | `/api/task` | `PATCH /{handlaggningId}` | Updates ersättningar. Body: `{"handlaggningId": "<uuid>", "ersattningar": [UpdateErsattning], "motivering": "..."}`. Returns 204. |
| `POST` | `/api/task/done` | `POST /{handlaggningId}/done` | Marks the task as done. Requires an `Authorization` header. Body: `{"handlaggningId": "<uuid>"}`. Returns 204. |
| `GET` | `/api/uppgiftsbeskrivning` | `GET /utokadUppgiftsbeskrivning` | Fetches the extended task description. |
| `GET` | `/api/health` | | Own liveness endpoint. |
| `GET` | `/q/health/ready` | `HEAD` on `BACKEND_URL` | Readiness, including backend reachability. |

`UpdateErsattning` follows the openapi spec: `ersattningId`, `omfattning_procent`, `belopp`, `berakningsgrund`,
`from`, `tom` are required; `beslutsutfall` (`JA`, `NEJ`, `FU`) and `avslagsanledning` are optional.

Invalid input (missing or non-UUID `handlaggningId`, empty `ersattningar`, missing `motivering`, an ersättning without a
required field) returns 400 without calling the backend. Backend errors return `{"error": "Upstream error"}` with
status 500.

## Container image

```shell script
./mvnw clean package
docker run -p 9009:9009 \
  -e BACKEND_URL=http://host.docker.internal:8080/regel/kommunicering \
  rimfrost/rimfrost-regel-kommunicering-bff
```
