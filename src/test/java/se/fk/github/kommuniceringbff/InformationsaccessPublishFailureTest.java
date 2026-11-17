package se.fk.github.kommuniceringbff;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.bff.ilogg.ILogg;
import se.fk.rimfrost.framework.bff.ilogg.exception.ILoggException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static se.fk.github.kommuniceringbff.KommuniceringBffControllerTest.HANDLAGGNING_ID;
import static se.fk.github.kommuniceringbff.KommuniceringBffControllerTest.stubGetData;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class InformationsaccessPublishFailureTest
{
   @InjectMock
   ILogg iLogg;

   @BeforeEach
   void setUp()
   {
      WireMockTestResource.getServer().resetAll();
   }

   @Test
   @DisplayName("GET /api/task/{handlaggningId} returnerar fel utan ärendeunderlag om eventet inte kan publiceras")
   void getTask_returnsErrorWithoutData_whenEventCannotBePublished() throws ILoggException
   {
      stubGetData();
      Mockito.doThrow(new ILoggException("Kafka unavailable", new RuntimeException("nack")))
            .when(iLogg).publish(Mockito.any());

      given()
            .when()
            .get("/api/task/" + HANDLAGGNING_ID)
            .then()
            .statusCode(500)
            .body("error", equalTo("Internal server error"))
            .body("kund", nullValue());
   }
}
