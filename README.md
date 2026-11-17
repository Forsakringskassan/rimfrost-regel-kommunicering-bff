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
| `GET` | `/api/task/{handlaggningId}` | `GET /{handlaggningId}` | Fetches kund and ersättningar. Returns the `data` part of the backend's response (`GetDataResponse`) and publishes an informationsaccess event. |
| `PATCH` | `/api/task/{handlaggningId}` | `PATCH /{handlaggningId}` | Updates ersättningar. Body: `{"ersattningar": [UpdateErsattning], "motivering": "..."}`. Returns 204. |
| `PATCH` | `/api/task/{handlaggningId}/kommuniceringsutfall` | `PATCH /kommuniceringsutfall/{handlaggningId}` | Registers the outcome of the kommunicering. Body: `{"utfall": "NekatBestridande" \| "AccepteratBestridande" \| "KundAccepterat"}`. Returns 204. |
| `PATCH` | `/api/task/{handlaggningId}/uppgiftskommentar` | `PATCH /uppgiftskommentar/{handlaggningId}` | Saves the handläggare's comment on the task. Body: `{"kommentar": "..."}`. Returns 204. |
| `POST` | `/api/task/{handlaggningId}/done` | `POST /{handlaggningId}/done` | Marks the task as done. No body. Returns 204. |
| `GET` | `/api/uppgiftsbeskrivning` | `GET /utokadUppgiftsbeskrivning` | Fetches the extended task description. |
| `GET` | `/api/health` | | Own liveness endpoint. |
| `GET` | `/q/health/ready` | `HEAD` on `BACKEND_URL` | Readiness, including backend reachability. |

`UpdateErsattning` follows the openapi spec: `ersattningId`, `omfattning_procent`, `belopp`, `berakningsgrund`,
`from`, `tom` are required; `beslutsutfall` (`JA`, `NEJ`, `FU`) and `avslagsanledning` are optional.

Every task endpoint takes `handlaggningId` in the path. The `Authorization` header is passed on to the backend, which
rejects calls without a valid token.

Invalid input (a non-UUID `handlaggningId`, empty `ersattningar`, missing `motivering`, an ersättning without a
required field, missing or unknown `utfall`, missing `kommentar`) returns 400 without calling the backend.

A backend error response is passed through with the backend's status code (for example 401 for a missing or invalid
token, 404, 500) and the body `{"error": "Upstream error"}`. A backend that cannot be reached gives 502 with
`{"error": "Upstream unavailable"}`. Any other failure gives 500 with `{"error": "Internal server error"}`.

## Informationsaccess

The backend wraps its `GET /{handlaggningId}` response as `{"data": ..., "informationsaccess": ...}`. For every
successful read the BFF publishes an `InformationsaccessEvent` on the `ilogg-event` channel (topic
`informationsaccess`), with `data` as `visadInformation`, and returns only `data` to the frontend. If the event cannot
be published the BFF answers 500 and sends no data. No other endpoint publishes an event.

## Container image

```shell script
./mvnw clean package
docker run -p 9009:9009 \
  -e BACKEND_URL=http://host.docker.internal:8080/regel/kommunicering \
  rimfrost/rimfrost-regel-kommunicering-bff
```
