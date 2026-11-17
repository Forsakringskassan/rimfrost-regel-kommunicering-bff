package se.fk.github.kommuniceringbff.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import se.fk.rimfrost.framework.regel.kommuniceringsutfall.jaxrsspec.controllers.generatedsource.model.PatchKommuniceringsutfallRequest;
import se.fk.rimfrost.framework.regel.uppgiftskommentar.jaxrsspec.controllers.generatedsource.model.PatchUppgiftskommentarRequest;
import se.fk.github.kommuniceringbff.model.BackendGetResponse;
import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

@RegisterRestClient(configKey = "backend")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface KommuniceringClient
{
   @GET
   @Path("/{handlaggningId}")
   BackendGetResponse getData(@PathParam("handlaggningId") String handlaggningId);

   @PATCH
   @Path("/{handlaggningId}")
   void patchErsattningar(
         @PathParam("handlaggningId") String handlaggningId,
         PatchErsattningRequest body);

   @POST
   @Path("/{handlaggningId}/done")
   void done(@PathParam("handlaggningId") String handlaggningId);

   @PATCH
   @Path("/kommuniceringsutfall/{handlaggningId}")
   void patchKommuniceringsutfall(
         @PathParam("handlaggningId") String handlaggningId,
         PatchKommuniceringsutfallRequest body);

   @PATCH
   @Path("/uppgiftskommentar/{handlaggningId}")
   void patchUppgiftskommentar(
         @PathParam("handlaggningId") String handlaggningId,
         PatchUppgiftskommentarRequest body);

   @GET
   @Path("/utokadUppgiftsbeskrivning")
   JsonNode getUppgiftsbeskrivning();
}
