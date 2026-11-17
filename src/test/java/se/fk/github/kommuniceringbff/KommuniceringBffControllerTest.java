package se.fk.github.kommuniceringbff;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.informationsaccess.InformationsaccessEvent;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class KommuniceringBffControllerTest
{
   static final String HANDLAGGNING_ID = "3f2b8c1e-6d4a-4b7e-9a0c-1e2d3f4a5b6c";
   private static final String ERSATTNING_ID = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
   private static final String UPPGIFT_ID = "8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d";
   private static final String REGEL_ID = "a11f3429-3a6b-4389-ba1c-c2747b0fb45b";
   private static final String ANVANDARE_TYP_ID = "9b1e4f2a-6c3d-4e8b-a7f5-2d0c1b9e8a36";

   private static final String BACKEND_DATA = """
         {
           "handlaggning_id": "%s",
           "kund": {"fornamn": "Anna", "efternamn": "Andersson", "kon": "KVINNA"},
           "ersattningar": [{
             "ersattning_id": "%s",
             "ersattningstyp": "VAB",
             "omfattning_procent": 100,
             "belopp": 500,
             "berakningsgrund": 2000,
             "beslutsutfall": "JA",
             "avslagsanledning": null,
             "from": "2026-02-01",
             "tom": "2026-02-28"
           }]
         }""".formatted(HANDLAGGNING_ID, ERSATTNING_ID);

   // The regeltjänst wraps its data together with informationsaccess data (FRKOMM-FR-08.1)
   static final String BACKEND_GET_RESPONSE = """
         {
           "data": %s,
           "informationsaccess": {
             "uppgiftId": "%s",
             "regelId": "%s",
             "regelVersion": "1",
             "anvandare": {"typId": "%s", "varde": "H123456"}
           }
         }""".formatted(BACKEND_DATA, UPPGIFT_ID, REGEL_ID, ANVANDARE_TYP_ID);

   private static final String UPDATE_ERSATTNING = """
         {"ersattningId": "%s", "omfattning_procent": 50, "belopp": 250, "berakningsgrund": 2000,
          "beslutsutfall": "NEJ", "avslagsanledning": "Ej uppfyllt", "from": "2026-02-01", "tom": "2026-02-28"}"""
         .formatted(ERSATTNING_ID);

   private static String patchBody(String ersattningar, String motivering)
   {
      return """
            {"ersattningar": %s, "motivering": %s}"""
            .formatted(ersattningar, motivering);
   }

   private static String utfallBody(String utfall)
   {
      return """
            {"utfall": %s}"""
            .formatted(utfall);
   }

   private static String kommentarBody(String kommentar)
   {
      return """
            {"kommentar": %s}"""
            .formatted(kommentar);
   }

   @Inject
   @Connector("smallrye-in-memory")
   InMemoryConnector inMemoryConnector;

   @BeforeEach
   void setUp()
   {
      WireMockTestResource.getServer().resetAll();
      inMemoryConnector.sink("ilogg-event").clear();
   }

   @Test
   @DisplayName("GET /api/health returnerar 200 med status ok")
   void health_returns200WithStatusOk()
   {
      given()
            .when()
            .get("/api/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("ok"))
            .body("timestamp", notNullValue());
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} hämtar kommuniceringsdata från backend")
   void getTask_returnsBackendData()
   {
      stubGetData();

      given()
            .when()
            .get("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(200)
            .body("handlaggning_id", equalTo(HANDLAGGNING_ID))
            .body("kund.fornamn", equalTo("Anna"))
            .body("kund.kon", equalTo("KVINNA"))
            .body("ersattningar[0].ersattning_id", equalTo(ERSATTNING_ID))
            .body("ersattningar[0].omfattning_procent", equalTo(100))
            .body("ersattningar[0].beslutsutfall", equalTo("JA"))
            .body("ersattningar[0].avslagsanledning", nullValue())
            .body("ersattningar[0].from", equalTo("2026-02-01"))
            .body("ersattningar[0].tom", equalTo("2026-02-28"))
            .body("data", nullValue())
            .body("informationsaccess", nullValue());
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} publicerar ett informationsaccess-event med det som visades")
   @SuppressWarnings("unchecked")
   void getTask_publishesInformationsaccessEvent()
   {
      stubGetData();

      given()
            .when()
            .get("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(200);

      var received = receivedEvents();
      assertEquals(1, received.size());
      var event = received.getFirst();
      assertNotNull(event.getId());
      assertEquals(ANVANDARE_TYP_ID, event.getAnvandare().getTypId());
      assertEquals("H123456", event.getAnvandare().getVarde());
      assertNotNull(event.getTidpunkt());
      assertEquals("rimfrost-regel-kommunicering-bff", event.getKalla());
      assertEquals(HANDLAGGNING_ID, event.getHandlaggningId());
      assertEquals(UPPGIFT_ID, event.getUppgiftId());
      assertEquals(REGEL_ID, event.getRegelId());
      assertEquals("1", event.getRegelVersion());
      var kund = (Map<String, Object>) event.getVisadInformation().get("kund");
      assertEquals("Anna", kund.get("fornamn"));
      assertTrue(event.getVisadInformation().containsKey("ersattningar"));
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} vidarebefordrar Authorization-headern till backend")
   void getTask_forwardsAuthorizationHeader()
   {
      stubGetData();

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(200);

      WireMockTestResource.getServer().verify(getRequestedFor(urlEqualTo("/" + HANDLAGGNING_ID))
            .withHeader("Authorization", WireMock.equalTo("Bearer test-token")));
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} två gånger ger två event med olika id")
   void getTask_publishesEventWithUniqueIdPerCall()
   {
      stubGetData();

      for (int i = 0; i < 2; i++)
      {
         given()
               .when()
               .get("/api/task/" + HANDLAGGNING_ID)
               .then()
               .statusCode(200);
      }

      var received = receivedEvents();
      assertEquals(2, received.size());
      assertNotEquals(received.get(0).getId(), received.get(1).getId());
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} med handlaggningId som inte är ett UUID returnerar 400")
   void getTask_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .when()
            .get("/api/task/task-123")
            .then()
            .statusCode(400);
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} returnerar 500 när backend fallerar")
   void getTask_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .when()
            .get("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));

      assertTrue(receivedEvents().isEmpty());
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId} skickar ersättningar och motivering till backend och returnerar 204")
   void patchTask_forwardsBodyAndReturns204()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .body(patchBody("[" + UPDATE_ERSATTNING + "]", "\"Kunden har kommunicerats\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(patchRequestedFor(urlEqualTo("/" + HANDLAGGNING_ID))
            .withRequestBody(matchingJsonPath("$.motivering", WireMock.equalTo("Kunden har kommunicerats")))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].ersattningId", WireMock.equalTo(ERSATTNING_ID)))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].omfattning_procent", WireMock.equalTo("50")))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].belopp", WireMock.equalTo("250")))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].beslutsutfall", WireMock.equalTo("NEJ")))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].avslagsanledning", WireMock.equalTo("Ej uppfyllt")))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].from", WireMock.equalTo("2026-02-01")))
            .withRequestBody(matchingJsonPath("$.ersattningar[0].tom", WireMock.equalTo("2026-02-28"))));
      WireMockTestResource.getServer().verify(patchRequestedFor(urlEqualTo("/" + HANDLAGGNING_ID))
            .withRequestBody(notContaining("handlaggningId")));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId} med handlaggningId som inte är ett UUID returnerar 400")
   void patchTask_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .contentType(ContentType.JSON)
            .body(patchBody("[" + UPDATE_ERSATTNING + "]", "\"motivering\""))
            .when()
            .patch("/api/task/task-123")
            .then()
            .statusCode(400);
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId} med tom ersättningslista returnerar 400")
   void patchTask_returns400_whenErsattningarIsEmpty()
   {
      given()
            .contentType(ContentType.JSON)
            .body(patchBody("[]", "\"motivering\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId} utan motivering returnerar 400")
   void patchTask_returns400_whenMotiveringIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(patchBody("[" + UPDATE_ERSATTNING + "]", "null"))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId} där en ersättning saknar obligatoriskt fält returnerar 400")
   void patchTask_returns400_whenErsattningMissesRequiredField()
   {
      String ersattningUtanBelopp = """
            {"ersattningId": "%s", "omfattning_procent": 50, "berakningsgrund": 2000,
             "from": "2026-02-01", "tom": "2026-02-28"}""".formatted(ERSATTNING_ID);

      given()
            .contentType(ContentType.JSON)
            .body(patchBody("[" + ersattningUtanBelopp + "]", "\"motivering\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId} returnerar 500 när backend fallerar")
   void patchTask_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(patchBody("[" + UPDATE_ERSATTNING + "]", "\"motivering\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/kommuniceringsutfall skickar utfallet till backend och returnerar 204")
   void patchKommuniceringsutfall_forwardsUtfallAndReturns204()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/kommuniceringsutfall/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .body(utfallBody("\"NekatBestridande\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/kommuniceringsutfall")
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(patchRequestedFor(urlEqualTo("/kommuniceringsutfall/" + HANDLAGGNING_ID))
            .withRequestBody(equalToJson("{\"utfall\": \"NekatBestridande\"}")));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/kommuniceringsutfall med handlaggningId som inte är ett UUID returnerar 400")
   void patchKommuniceringsutfall_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .contentType(ContentType.JSON)
            .body(utfallBody("\"KundAccepterat\""))
            .when()
            .patch("/api/task/task-123/kommuniceringsutfall")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/kommuniceringsutfall utan utfall returnerar 400")
   void patchKommuniceringsutfall_returns400_whenUtfallIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(utfallBody("null"))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/kommuniceringsutfall")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/kommuniceringsutfall med okänt utfall returnerar 400")
   void patchKommuniceringsutfall_returns400_whenUtfallIsUnknown()
   {
      given()
            .contentType(ContentType.JSON)
            .body(utfallBody("\"Okant\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/kommuniceringsutfall")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/kommuniceringsutfall returnerar 500 när backend fallerar")
   void patchKommuniceringsutfall_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/kommuniceringsutfall/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(utfallBody("\"AccepteratBestridande\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/kommuniceringsutfall")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/uppgiftskommentar skickar kommentaren till backend och returnerar 204")
   void patchUppgiftskommentar_forwardsKommentarAndReturns204()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/uppgiftskommentar/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody("\"Kunden har ringt\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/uppgiftskommentar")
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(patchRequestedFor(urlEqualTo("/uppgiftskommentar/" + HANDLAGGNING_ID))
            .withRequestBody(equalToJson("{\"kommentar\": \"Kunden har ringt\"}")));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/uppgiftskommentar med handlaggningId som inte är ett UUID returnerar 400")
   void patchUppgiftskommentar_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody("\"kommentar\""))
            .when()
            .patch("/api/task/task-123/uppgiftskommentar")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/uppgiftskommentar utan kommentar returnerar 400")
   void patchUppgiftskommentar_returns400_whenKommentarIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody("null"))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/uppgiftskommentar")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/{handlaggningId}/uppgiftskommentar returnerar 500 när backend fallerar")
   void patchUppgiftskommentar_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/uppgiftskommentar/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody("\"kommentar\""))
            .when()
            .patch("/api/task/" + HANDLAGGNING_ID + "/uppgiftskommentar")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("POST /api/task/{handlaggningId}/done markerar uppgiften som klar i backend och returnerar 204")
   void taskDone_returns204_onSuccess()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + HANDLAGGNING_ID + "/done"))
            .willReturn(aResponse().withStatus(204)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .post("/api/task/" + HANDLAGGNING_ID + "/done")
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(postRequestedFor(urlEqualTo("/" + HANDLAGGNING_ID + "/done"))
            .withHeader("Authorization", WireMock.equalTo("Bearer test-token")));
   }

   @Test
   @DisplayName("POST /api/task/{handlaggningId}/done med handlaggningId som inte är ett UUID returnerar 400")
   void taskDone_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .when()
            .post("/api/task/task-123/done")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, postRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("POST /api/task/{handlaggningId}/done returnerar 500 när backend fallerar")
   void taskDone_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + HANDLAGGNING_ID + "/done"))
            .willReturn(aResponse().withStatus(500)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .post("/api/task/" + HANDLAGGNING_ID + "/done")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("GET /api/uppgiftsbeskrivning hämtar utökad uppgiftsbeskrivning från backend")
   void getUppgiftsbeskrivning_returnsData()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/utokadUppgiftsbeskrivning"))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("{\"beskrivning\": \"Hantera kommunicering från kund\"}")));

      given()
            .when()
            .get("/api/uppgiftsbeskrivning")
            .then()
            .statusCode(200)
            .body("beskrivning", equalTo("Hantera kommunicering från kund"));

      assertTrue(receivedEvents().isEmpty());
   }

   @Test
   @DisplayName("GET /api/uppgiftsbeskrivning returnerar 500 när anslutningen till backend bryts")
   void getUppgiftsbeskrivning_returns500_whenBackendConnectionReset()
   {
      // See the same test in rimfrost-template-micro-fe-bff: a connection reset currently falls
      // through to the framework's generic 500 branch rather than 502.
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/utokadUppgiftsbeskrivning"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

      given()
            .when()
            .get("/api/uppgiftsbeskrivning")
            .then()
            .statusCode(500)
            .body("error", equalTo("Internal server error"));
   }

   @Test
   @DisplayName("Readiness rapporterar backend UP när backend svarar")
   void readiness_reportsBackendUp()
   {
      WireMockTestResource.getServer().stubFor(head(urlMatching(".*"))
            .willReturn(aResponse().withStatus(200)));

      given()
            .when()
            .get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("checks.find { it.name == 'backend' }.status", equalTo("UP"));
   }

   @Test
   @DisplayName("Readiness rapporterar backend DOWN när backend inte nås")
   void readiness_reportsBackendDown_whenBackendUnreachable()
   {
      WireMockTestResource.getServer().stubFor(head(urlMatching(".*"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

      given()
            .when()
            .get("/q/health/ready")
            .then()
            .statusCode(503)
            .body("status", equalTo("DOWN"))
            .body("checks.find { it.name == 'backend' }.status", equalTo("DOWN"));
   }

   static void stubGetData()
   {
      stubGetDataWithBody(BACKEND_GET_RESPONSE);
   }

   private static void stubGetDataWithBody(String body)
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody(body)));
   }

   private List<InformationsaccessEvent> receivedEvents()
   {
      return inMemoryConnector.sink("ilogg-event").received().stream()
            .map(message -> (InformationsaccessEvent) message.getPayload())
            .toList();
   }
}
