package se.fk.github.kommuniceringbff;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.github.kommuniceringbff.integration.InformationsaccessPublisher;
import se.fk.github.kommuniceringbff.integration.KommuniceringClient;
import se.fk.github.kommuniceringbff.model.BackendGetResponse;
import se.fk.github.kommuniceringbff.model.KommuniceringsutfallRequest;
import se.fk.github.kommuniceringbff.model.PatchTaskRequest;
import se.fk.github.kommuniceringbff.model.UppgiftskommentarRequest;
import se.fk.rimfrost.framework.bff.logging.LogContext;
import se.fk.rimfrost.framework.regel.kommuniceringsutfall.jaxrsspec.controllers.generatedsource.model.PatchKommuniceringsutfallRequest;
import se.fk.rimfrost.framework.regel.uppgiftskommentar.jaxrsspec.controllers.generatedsource.model.PatchUppgiftskommentarRequest;
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

   @Inject
   InformationsaccessPublisher informationsaccessPublisher;

   @GET
   @Path("/health")
   public Response health()
   {
      return Response.ok(Map.of("status", "ok", "timestamp", Instant.now().toString())).build();
   }

   @GET
   @Path("/task/{handlaggningId}")
   public Response getTask(
         @PathParam("handlaggningId") String handlaggningId)
   {
      LOGGER.debug("GET /api/task/{}", handlaggningId);
      return withLogContext(handlaggningId, () -> {
         BackendGetResponse response = backendClient.getData(handlaggningId);
         informationsaccessPublisher.publish(handlaggningId, response);
         return Response.ok(response.data()).build();
      });
   }

   @PATCH
   @Path("/task/{handlaggningId}")
   public Response patchTask(
         @PathParam("handlaggningId") String handlaggningId,
         @Valid @NotNull PatchTaskRequest body)
   {
      LOGGER.debug("PATCH /api/task/{}", handlaggningId);
      return withLogContext(handlaggningId, () -> {
         PatchErsattningRequest backendBody = new PatchErsattningRequest()
               .ersattningar(body.ersattningar())
               .motivering(body.motivering());
         backendClient.patchErsattningar(handlaggningId, backendBody);
         return Response.noContent().build();
      });
   }

   @PATCH
   @Path("/task/{handlaggningId}/kommuniceringsutfall")
   public Response patchKommuniceringsutfall(
         @PathParam("handlaggningId") String handlaggningId,
         @Valid @NotNull KommuniceringsutfallRequest body)
   {
      LOGGER.debug("PATCH /api/task/{}/kommuniceringsutfall", handlaggningId);
      return withLogContext(handlaggningId, () -> {
         backendClient.patchKommuniceringsutfall(handlaggningId,
               new PatchKommuniceringsutfallRequest().utfall(body.utfall()));
         return Response.noContent().build();
      });
   }

   @PATCH
   @Path("/task/{handlaggningId}/uppgiftskommentar")
   public Response patchUppgiftskommentar(
         @PathParam("handlaggningId") String handlaggningId,
         @Valid @NotNull UppgiftskommentarRequest body)
   {
      LOGGER.debug("PATCH /api/task/{}/uppgiftskommentar", handlaggningId);
      return withLogContext(handlaggningId, () -> {
         backendClient.patchUppgiftskommentar(handlaggningId,
               new PatchUppgiftskommentarRequest().kommentar(body.kommentar()));
         return Response.noContent().build();
      });
   }

   @POST
   @Path("/task/{handlaggningId}/done")
   // No body, so a client sends no Content-Type; the class-level JSON @Consumes would answer 415
   @Consumes(MediaType.WILDCARD)
   public Response taskDone(
         @PathParam("handlaggningId") String handlaggningId)
   {
      LOGGER.debug("POST /api/task/{}/done", handlaggningId);
      return withLogContext(handlaggningId, () -> {
         backendClient.done(handlaggningId);
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
