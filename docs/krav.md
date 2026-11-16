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

### KOMBFF-FR-02 — Uppdatera ersättningar

- **KOMBFF-FR-02.1** BFF:en ska skicka uppdaterade ersättningar och en motivering till regeltjänsten
  enligt `PatchErsattningRequest`.

### KOMBFF-FR-03 — Slutföra uppgift

- **KOMBFF-FR-03.1** BFF:en ska markera uppgiften som klar hos regeltjänsten.
- **KOMBFF-FR-03.2** BFF:en ska neka slutförande med 401 om `Authorization`-header saknas.

### KOMBFF-FR-04 — Uppgiftsbeskrivning

- **KOMBFF-FR-04.1** BFF:en ska hämta den utökade uppgiftsbeskrivningen från regeltjänsten.

### KOMBFF-FR-05 — Validering och felhantering

- **KOMBFF-FR-05.1** BFF:en ska svara 400 utan att anropa regeltjänsten när `handlaggningId` saknas eller
  inte är ett UUID, när `ersattningar` är tom, när `motivering` saknas eller när en ersättning saknar ett
  obligatoriskt fält.
- **KOMBFF-FR-05.2** Fel från regeltjänsten ska ge ett enhetligt felsvar utan interna feldetaljer.

---

## Icke-funktionella krav

### KOMBFF-NFR-01 — Driftbarhet

- **KOMBFF-NFR-01.1** BFF:en ska rapportera readiness baserat på om regeltjänsten går att nå.

### KOMBFF-NFR-02 — Testbarhet

- **KOMBFF-NFR-02.1** Varje ändpunkt ska ha integrationstester mot en simulerad regeltjänst för normalfall,
  valideringsfel och fel från regeltjänsten.
