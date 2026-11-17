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
├── integration/InformationsaccessPublisher # Publicerar informationsaccess-event via ILogg
├── health/HealthCheckProducer         # Readiness mot regeltjänsten
└── model/                             # Request-modeller, BackendGetResponse, HandlaggningIdFormat
```

## API

| Metod | Sökväg | Anrop mot regeltjänsten | Beskrivning |
|---|---|---|---|
| GET | `/api/health` | – | Egen hälsokontroll |
| GET | `/api/task/{handlaggningId}` | `GET /{handlaggningId}` | Hämtar kund och ersättningar (`GetDataResponse`) och publicerar informationsaccess-event |
| PATCH | `/api/task/{handlaggningId}` | `PATCH /{handlaggningId}` | Uppdaterar ersättningar med motivering (`PatchErsattningRequest`) |
| PATCH | `/api/task/{handlaggningId}/kommuniceringsutfall` | `PATCH /kommuniceringsutfall/{handlaggningId}` | Registrerar kommuniceringsutfall (`PatchKommuniceringsutfallRequest`) |
| PATCH | `/api/task/{handlaggningId}/uppgiftskommentar` | `PATCH /uppgiftskommentar/{handlaggningId}` | Sparar uppgiftskommentar (`PatchUppgiftskommentarRequest`) |
| POST | `/api/task/{handlaggningId}/done` | `POST /{handlaggningId}/done` | Markerar uppgiften som klar, utan body |
| GET | `/api/uppgiftsbeskrivning` | `GET /utokadUppgiftsbeskrivning` | Hämtar utökad uppgiftsbeskrivning |

Alla uppgiftsanrop tar `handlaggningId` i sökvägen, som regeltjänsten, och läsningen är GET hela vägen,
frontend → BFF → regeltjänst. BFF:en validerar att `handlaggningId` är ett UUID. `Authorization` vidarebefordras
till regeltjänsten av `rimfrost-framework-bff`, och regeltjänsten nekar anrop utan giltig token.

Modellerna för kommuniceringsutfall och uppgiftskommentar kommer från
`rimfrost-framework-regel-kommuniceringsutfall-openapi-jaxrs-spec` och
`rimfrost-framework-regel-uppgiftskommentar-openapi-jaxrs-spec`. Regeltjänsten exponerar dem via
`rimfrost-framework-regel-kommunicering`.

## Kafka-integration

Utgående kanal `ilogg-event` (topic `informationsaccess`) för informationsåtkomsthändelser, konfigurerad
som i mallen. Regeltjänstens GET-svar är en container med `data` och `informationsaccess`
(FRKOMM-FR-08.1). Vid varje lyckad läsning publicerar `InformationsaccessPublisher` ett
`InformationsaccessEvent` med `data` som `visadInformation`, och bara `data` returneras till frontenden. Kan
eventet inte publiceras svarar BFF:en 500 utan data, som i `rimfrost-regel-rtf-manuell-bff`.

## Felhantering

`GlobalExceptionMapper` i `rimfrost-framework-bff` skickar vidare regeltjänstens statuskod med kroppen
`{"error": "Upstream error"}`, t.ex. 401 när token saknas. En regeltjänst som inte går att nå ger 502
(`Upstream unavailable`), övriga fel 500 (`Internal server error`).

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
