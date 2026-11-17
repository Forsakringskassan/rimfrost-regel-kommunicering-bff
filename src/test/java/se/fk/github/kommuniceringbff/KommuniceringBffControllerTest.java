package se.fk.github.kommuniceringbff;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class KommuniceringBffControllerTest
{
   private static final String HANDLAGGNING_ID = "3f2b8c1e-6d4a-4b7e-9a0c-1e2d3f4a5b6c";
   private static final String ERSATTNING_ID = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

   private static final String BACKEND_GET_RESPONSE = """
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

   private static final String UPDATE_ERSATTNING = """
         {"ersattningId": "%s", "omfattning_procent": 50, "belopp": 250, "berakningsgrund": 2000,
          "beslutsutfall": "NEJ", "avslagsanledning": "Ej uppfyllt", "from": "2026-02-01", "tom": "2026-02-28"}"""
         .formatted(ERSATTNING_ID);

   private static String taskBody(String handlaggningId)
   {
      return "{\"handlaggningId\": \"%s\"}".formatted(handlaggningId);
   }

   private static String patchBody(String handlaggningId, String ersattningar, String motivering)
   {
      return """
            {"handlaggningId": "%s", "ersattningar": %s, "motivering": %s}"""
            .formatted(handlaggningId, ersattningar, motivering);
   }

   private static String utfallBody(String handlaggningId, String utfall)
   {
      return """
            {"handlaggningId": "%s", "utfall": %s}"""
            .formatted(handlaggningId, utfall);
   }

   private static String kommentarBody(String handlaggningId, String kommentar)
   {
      return """
            {"handlaggningId": "%s", "kommentar": %s}"""
            .formatted(handlaggningId, kommentar);
   }

   @BeforeEach
   void setUp()
   {
      WireMockTestResource.getServer().resetAll();
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
   @DisplayName("POST /api/task hämtar kommuniceringsdata från backend")
   void getTask_returnsBackendData()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody(BACKEND_GET_RESPONSE)));

      given()
            .contentType(ContentType.JSON)
            .body(taskBody(HANDLAGGNING_ID))
            .when()
            .post("/api/task")
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
            .body("ersattningar[0].tom", equalTo("2026-02-28"));
   }

   @Test
   @DisplayName("POST /api/task utan handlaggningId returnerar 400")
   void getTask_returns400_whenHandlaggningIdIsBlank()
   {
      given()
            .contentType(ContentType.JSON)
            .body(taskBody(""))
            .when()
            .post("/api/task")
            .then()
            .statusCode(400);
   }

   @Test
   @DisplayName("POST /api/task med handlaggningId som inte är ett UUID returnerar 400")
   void getTask_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .contentType(ContentType.JSON)
            .body(taskBody("task-123"))
            .when()
            .post("/api/task")
            .then()
            .statusCode(400);
   }

   @Test
   @DisplayName("POST /api/task returnerar 500 när backend fallerar")
   void getTask_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(taskBody(HANDLAGGNING_ID))
            .when()
            .post("/api/task")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("PATCH /api/task skickar ersättningar och motivering till backend och returnerar 204")
   void patchTask_forwardsBodyAndReturns204()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .body(patchBody(HANDLAGGNING_ID, "[" + UPDATE_ERSATTNING + "]", "\"Kunden har kommunicerats\""))
            .when()
            .patch("/api/task")
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
   @DisplayName("PATCH /api/task utan handlaggningId returnerar 400")
   void patchTask_returns400_whenHandlaggningIdIsBlank()
   {
      given()
            .contentType(ContentType.JSON)
            .body(patchBody("", "[" + UPDATE_ERSATTNING + "]", "\"motivering\""))
            .when()
            .patch("/api/task")
            .then()
            .statusCode(400);
   }

   @Test
   @DisplayName("PATCH /api/task med tom ersättningslista returnerar 400")
   void patchTask_returns400_whenErsattningarIsEmpty()
   {
      given()
            .contentType(ContentType.JSON)
            .body(patchBody(HANDLAGGNING_ID, "[]", "\"motivering\""))
            .when()
            .patch("/api/task")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task utan motivering returnerar 400")
   void patchTask_returns400_whenMotiveringIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(patchBody(HANDLAGGNING_ID, "[" + UPDATE_ERSATTNING + "]", "null"))
            .when()
            .patch("/api/task")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task där en ersättning saknar obligatoriskt fält returnerar 400")
   void patchTask_returns400_whenErsattningMissesRequiredField()
   {
      String ersattningUtanBelopp = """
            {"ersattningId": "%s", "omfattning_procent": 50, "berakningsgrund": 2000,
             "from": "2026-02-01", "tom": "2026-02-28"}""".formatted(ERSATTNING_ID);

      given()
            .contentType(ContentType.JSON)
            .body(patchBody(HANDLAGGNING_ID, "[" + ersattningUtanBelopp + "]", "\"motivering\""))
            .when()
            .patch("/api/task")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task returnerar 500 när backend fallerar")
   void patchTask_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(patchBody(HANDLAGGNING_ID, "[" + UPDATE_ERSATTNING + "]", "\"motivering\""))
            .when()
            .patch("/api/task")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("PATCH /api/task/kommuniceringsutfall skickar utfallet till backend och returnerar 204")
   void patchKommuniceringsutfall_forwardsUtfallAndReturns204()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/kommuniceringsutfall/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .body(utfallBody(HANDLAGGNING_ID, "\"NekatBestridande\""))
            .when()
            .patch("/api/task/kommuniceringsutfall")
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(patchRequestedFor(urlEqualTo("/kommuniceringsutfall/" + HANDLAGGNING_ID))
            .withRequestBody(equalToJson("{\"utfall\": \"NekatBestridande\"}")));
   }

   @Test
   @DisplayName("PATCH /api/task/kommuniceringsutfall med handlaggningId som inte är ett UUID returnerar 400")
   void patchKommuniceringsutfall_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .contentType(ContentType.JSON)
            .body(utfallBody("task-123", "\"KundAccepterat\""))
            .when()
            .patch("/api/task/kommuniceringsutfall")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/kommuniceringsutfall utan utfall returnerar 400")
   void patchKommuniceringsutfall_returns400_whenUtfallIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(utfallBody(HANDLAGGNING_ID, "null"))
            .when()
            .patch("/api/task/kommuniceringsutfall")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/kommuniceringsutfall med okänt utfall returnerar 400")
   void patchKommuniceringsutfall_returns400_whenUtfallIsUnknown()
   {
      given()
            .contentType(ContentType.JSON)
            .body(utfallBody(HANDLAGGNING_ID, "\"Okant\""))
            .when()
            .patch("/api/task/kommuniceringsutfall")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/kommuniceringsutfall returnerar 500 när backend fallerar")
   void patchKommuniceringsutfall_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/kommuniceringsutfall/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(utfallBody(HANDLAGGNING_ID, "\"AccepteratBestridande\""))
            .when()
            .patch("/api/task/kommuniceringsutfall")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("PATCH /api/task/uppgiftskommentar skickar kommentaren till backend och returnerar 204")
   void patchUppgiftskommentar_forwardsKommentarAndReturns204()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/uppgiftskommentar/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody(HANDLAGGNING_ID, "\"Kunden har ringt\""))
            .when()
            .patch("/api/task/uppgiftskommentar")
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(patchRequestedFor(urlEqualTo("/uppgiftskommentar/" + HANDLAGGNING_ID))
            .withRequestBody(equalToJson("{\"kommentar\": \"Kunden har ringt\"}")));
   }

   @Test
   @DisplayName("PATCH /api/task/uppgiftskommentar med handlaggningId som inte är ett UUID returnerar 400")
   void patchUppgiftskommentar_returns400_whenHandlaggningIdIsNotUuid()
   {
      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody("task-123", "\"kommentar\""))
            .when()
            .patch("/api/task/uppgiftskommentar")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/uppgiftskommentar utan kommentar returnerar 400")
   void patchUppgiftskommentar_returns400_whenKommentarIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody(HANDLAGGNING_ID, "null"))
            .when()
            .patch("/api/task/uppgiftskommentar")
            .then()
            .statusCode(400);

      WireMockTestResource.getServer().verify(0, patchRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("PATCH /api/task/uppgiftskommentar returnerar 500 när backend fallerar")
   void patchUppgiftskommentar_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/uppgiftskommentar/" + HANDLAGGNING_ID))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .body(kommentarBody(HANDLAGGNING_ID, "\"kommentar\""))
            .when()
            .patch("/api/task/uppgiftskommentar")
            .then()
            .statusCode(500)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   @DisplayName("POST /api/task/done markerar uppgiften som klar i backend och returnerar 204")
   void taskDone_returns204_onSuccess()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + HANDLAGGNING_ID + "/done"))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body(taskBody(HANDLAGGNING_ID))
            .when()
            .post("/api/task/done")
            .then()
            .statusCode(204);

      WireMockTestResource.getServer().verify(postRequestedFor(urlEqualTo("/" + HANDLAGGNING_ID + "/done")));
   }

   @Test
   @DisplayName("POST /api/task/done utan Authorization-header returnerar 401")
   void taskDone_returns401_whenAuthorizationMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .body(taskBody(HANDLAGGNING_ID))
            .when()
            .post("/api/task/done")
            .then()
            .statusCode(401)
            .body("error", equalTo("Authorization header required"));

      WireMockTestResource.getServer().verify(0, postRequestedFor(anyUrl()));
   }

   @Test
   @DisplayName("POST /api/task/done returnerar 500 när backend fallerar")
   void taskDone_returns500_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + HANDLAGGNING_ID + "/done"))
            .willReturn(aResponse().withStatus(500)));

      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body(taskBody(HANDLAGGNING_ID))
            .when()
            .post("/api/task/done")
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
}
