package se.fk.github.kommuniceringbff;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.github.kommuniceringbff.integration.KommuniceringClient;
import se.fk.github.kommuniceringbff.model.KommuniceringsutfallRequest;
import se.fk.github.kommuniceringbff.model.PatchTaskRequest;
import se.fk.github.kommuniceringbff.model.TaskRequest;
import se.fk.github.kommuniceringbff.model.UppgiftskommentarRequest;
import se.fk.rimfrost.framework.bff.errorhandling.ErrorResponse;
import se.fk.rimfrost.framework.bff.logging.LogContext;
import se.fk.rimfrost.framework.regel.kommuniceringsutfall.jaxrsspec.controllers.generatedsource.model.PatchKommuniceringsutfallRequest;
import se.fk.rimfrost.framework.regel.uppgiftskommentar.jaxrsspec.controllers.generatedsource.model.PatchUppgiftskommentarRequest;
import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KommuniceringBffController
{
   private static final Logger LOGGER = LoggerFactory.getLogger(KommuniceringBffController.class);

   @RestClient
   KommuniceringClient backendClient;

   @GET
   @Path("/health")
   public Response health()
   {
      return Response.ok(Map.of("status", "ok", "timestamp", Instant.now().toString())).build();
   }

   @POST
   @Path("/task")
   public Response getTask(@Valid TaskRequest body)
   {
      LOGGER.debug("POST /api/task handlaggningId={}", body.handlaggningId());
      return withLogContext(body.handlaggningId(), () -> {
         GetDataResponse data = backendClient.getData(body.handlaggningId());
         return Response.ok(data).build();
      });
   }

   @PATCH
   @Path("/task")
   public Response patchTask(@Valid PatchTaskRequest body)
   {
      LOGGER.debug("PATCH /api/task handlaggningId={}", body.handlaggningId());
      return withLogContext(body.handlaggningId(), () -> {
         PatchErsattningRequest backendBody = new PatchErsattningRequest()
               .ersattningar(body.ersattningar())
               .motivering(body.motivering());
         backendClient.patchErsattningar(body.handlaggningId(), backendBody);
         return Response.noContent().build();
      });
   }

   @PATCH
   @Path("/task/kommuniceringsutfall")
   public Response patchKommuniceringsutfall(@Valid KommuniceringsutfallRequest body)
   {
      LOGGER.debug("PATCH /api/task/kommuniceringsutfall handlaggningId={}", body.handlaggningId());
      return withLogContext(body.handlaggningId(), () -> {
         backendClient.patchKommuniceringsutfall(body.handlaggningId(),
               new PatchKommuniceringsutfallRequest().utfall(body.utfall()));
         return Response.noContent().build();
      });
   }

   @PATCH
   @Path("/task/uppgiftskommentar")
   public Response patchUppgiftskommentar(@Valid UppgiftskommentarRequest body)
   {
      LOGGER.debug("PATCH /api/task/uppgiftskommentar handlaggningId={}", body.handlaggningId());
      return withLogContext(body.handlaggningId(), () -> {
         backendClient.patchUppgiftskommentar(body.handlaggningId(),
               new PatchUppgiftskommentarRequest().kommentar(body.kommentar()));
         return Response.noContent().build();
      });
   }

   @POST
   @Path("/task/done")
   public Response taskDone(@Valid TaskRequest body, @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization)
   {
      LOGGER.debug("POST /api/task/done handlaggningId={}", body.handlaggningId());
      if (authorization == null || authorization.isBlank())
      {
         return Response.status(401).entity(new ErrorResponse("Authorization header required")).build();
      }
      return withLogContext(body.handlaggningId(), () -> {
         backendClient.done(body.handlaggningId());
         return Response.noContent().build();
      });
   }

   @GET
   @Path("/uppgiftsbeskrivning")
   public Response getUppgiftsbeskrivning()
   {
      LOGGER.debug("GET /api/uppgiftsbeskrivning");
      JsonNode data = backendClient.getUppgiftsbeskrivning();
      return Response.ok(data).build();
   }

   // Exceptions are left to propagate to the framework's GlobalExceptionMapper; this only
   // scopes the handlaggningId MDC key for the duration of the call.
   private Response withLogContext(String handlaggningId, Supplier<Response> action)
   {
      try (LogContext ignored = LogContext.put("handlaggningId", handlaggningId))
      {
         return action.get();
      }
   }
}
