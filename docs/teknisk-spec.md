# Teknisk spec — BFF för kommunicering (KOMBFF)

## Översikt

Synkron REST-BFF i Quarkus för mikrofrontenden för regeln Kommunicering. Ingen egen datalagring.
Anropar `rimfrost-regel-kommunicering` via en REST-klient och använder de modeller som genereras från
`rimfrost-regel-kommunicering-openapi` (`rimfrost-regel-kommunicering-openapi-jaxrs-spec`). Felmappning,
loggkontext och uppströms hälsokontroll kommer från `rimfrost-framework-bff`.

## Komponentstruktur

```text
src/main/java/se/fk/github/kommuniceringbff
├── KommuniceringBffController        # BFF-ändpunkter under /api
├── integration/KommuniceringClient    # REST-klient mot regeltjänsten
├── health/HealthCheckProducer         # Readiness mot regeltjänsten
└── model/                             # TaskRequest, PatchTaskRequest, HandlaggningIdFormat
```

## API

| Metod | Sökväg | Anrop mot regeltjänsten | Beskrivning |
|---|---|---|---|
| GET | `/api/health` | – | Egen hälsokontroll |
| POST | `/api/task` | `GET /{handlaggningId}` | Hämtar kund och ersättningar (`GetDataResponse`) |
| PATCH | `/api/task` | `PATCH /{handlaggningId}` | Uppdaterar ersättningar med motivering (`PatchErsattningRequest`) |
| POST | `/api/task/done` | `POST /{handlaggningId}/done` | Markerar uppgiften som klar, kräver `Authorization` |
| GET | `/api/uppgiftsbeskrivning` | `GET /utokadUppgiftsbeskrivning` | Hämtar utökad uppgiftsbeskrivning |

`handlaggningId` skickas i förfrågans kropp, som i mallen och mikrofrontend-mallen, och läggs i sökvägen
mot regeltjänsten. BFF:en validerar att det är ett UUID.

## Kafka-integration

Utgående kanal `ilogg-event` (topic `informationsaccess`) för informationsåtkomsthändelser, konfigurerad
som i mallen.

## Konfiguration

| Egenskap | Beskrivning | Standardvärde |
|---|---|---|
| `quarkus.rest-client.backend.url` (`BACKEND_URL`) | Bas-URL till regeltjänsten inklusive regelsökväg | `http://localhost:8080/regel/kommunicering` |
| `CORS_ORIGINS` | Tillåtna ursprung för CORS | Lokala mikrofrontend-portar |
| `CORS_ALLOW_CREDENTIALS` | Tillåt cookies över ursprung | `false` |
| `quarkus.http.port` | Lyssningsport | `9009` |

## Liveness och readiness

`/api/health` (egen) och `/q/health` från ramverket. Readiness kontrollerar att regeltjänsten svarar.

## Kända begränsningar

| Begränsning | Föreslagen åtgärd |
|---|---|
| Porten 9009 är mallens standard och inte samordnad med andra BFF:er | Bestäm port vid driftsättning |
| Topic för `ilogg-event` är kopierad från mallen | Bekräfta rätt topic |
| `Authorization` krävs för `/api/task/done` men vidarebefordras inte till regeltjänsten (samma som i mallen) | Inför header-propagering om regeltjänsten börjar kräva den |
