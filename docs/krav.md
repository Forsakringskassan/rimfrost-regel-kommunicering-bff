# Krav — BFF för kommunicering (KOMBFF)

## Bakgrund och syfte

BFF:en är mellanlagret mellan mikrofrontenden för regeln Kommunicering och regeltjänsten
`rimfrost-regel-kommunicering`. Handläggaren ska kunna se kund och ersättningar, justera ersättningar
med en motivering och markera uppgiften som klar.

---

## Intressenter och aktörer

| Aktör | Roll |
|---|---|
| Mikrofrontend för kommunicering | Anropar BFF:en |
| `rimfrost-regel-kommunicering` | Regeltjänst som BFF:en anropar |

---

## Funktionella krav

### KOMBFF-FR-01 — Hämta underlag

- **KOMBFF-FR-01.1** BFF:en ska hämta kund och ersättningar för ett handläggningsärende från regeltjänsten
  och returnera dem enligt `GetDataResponse` i `rimfrost-regel-kommunicering-openapi`.
- **KOMBFF-FR-01.2** Hämtningen ska vara en GET. Alla uppgiftsanrop ska ta `handlaggningId` i sökvägen.

### KOMBFF-FR-02 — Uppdatera ersättningar

- **KOMBFF-FR-02.1** BFF:en ska skicka uppdaterade ersättningar och en motivering till regeltjänsten
  enligt `PatchErsattningRequest`.

### KOMBFF-FR-03 — Slutföra uppgift

- **KOMBFF-FR-03.1** BFF:en ska markera uppgiften som klar hos regeltjänsten.
- **KOMBFF-FR-03.2** BFF:en ska vidarebefordra `Authorization`-headern till regeltjänsten, som nekar
  slutförande utan giltig token.

### KOMBFF-FR-04 — Uppgiftsbeskrivning

- **KOMBFF-FR-04.1** BFF:en ska hämta den utökade uppgiftsbeskrivningen från regeltjänsten.

### KOMBFF-FR-05 — Validering och felhantering

- **KOMBFF-FR-05.1** BFF:en ska svara 400 utan att anropa regeltjänsten när `handlaggningId` inte är ett
  UUID, när `ersattningar` är tom, när `motivering` saknas, när en ersättning saknar ett
  obligatoriskt fält, när `utfall` saknas eller är okänt eller när `kommentar` saknas.
- **KOMBFF-FR-05.2** Fel från regeltjänsten ska ge ett enhetligt felsvar utan interna feldetaljer.

### KOMBFF-FR-06 — Kommuniceringsutfall

- **KOMBFF-FR-06.1** BFF:en ska skicka handläggarens kommuniceringsutfall till regeltjänsten enligt
  `PatchKommuniceringsutfallRequest` i `rimfrost-framework-regel-kommuniceringsutfall-openapi`.

### KOMBFF-FR-07 — Uppgiftskommentar

- **KOMBFF-FR-07.1** BFF:en ska skicka handläggarens kommentar till regeltjänsten enligt
  `PatchUppgiftskommentarRequest` i `rimfrost-framework-regel-uppgiftskommentar-openapi`.

### KOMBFF-FR-08 — Informationsaccess

- **KOMBFF-FR-08.1** Vid varje hämtning av underlag ska BFF:en publicera ett informationsaccess-event enligt
  `rimfrost-service-informationsaccess-asyncapi`, byggt av regeltjänstens `informationsaccess`, med `data`
  som `visadInformation`. Varje event ska ha ett eget `id`.
- **KOMBFF-FR-08.2** Informationsaccessdata ska inte skickas vidare till frontenden.
- **KOMBFF-FR-08.3** Om eventet inte kan publiceras ska BFF:en svara med fel och inte returnera underlaget.
- **KOMBFF-FR-08.4** Anrop som inte returnerar personuppgifter, t.ex. hämtning av uppgiftsbeskrivning, ska
  inte ge något event.

---

## Icke-funktionella krav

### KOMBFF-NFR-01 — Driftbarhet

- **KOMBFF-NFR-01.1** BFF:en ska rapportera readiness baserat på om regeltjänsten går att nå.

### KOMBFF-NFR-02 — Testbarhet

- **KOMBFF-NFR-02.1** Varje ändpunkt ska ha integrationstester mot en simulerad regeltjänst för normalfall,
  valideringsfel och fel från regeltjänsten.
