package se.fk.github.kommuniceringbff.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

@RegisterRestClient(configKey = "backend")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface KommuniceringClient
{
   @GET
   @Path("/{handlaggningId}")
   GetDataResponse getData(@PathParam("handlaggningId") String handlaggningId);

   @PATCH
   @Path("/{handlaggningId}")
   void patchErsattningar(
         @PathParam("handlaggningId") String handlaggningId,
         PatchErsattningRequest body);

   @POST
   @Path("/{handlaggningId}/done")
   void done(@PathParam("handlaggningId") String handlaggningId);

   @GET
   @Path("/utokadUppgiftsbeskrivning")
   JsonNode getUppgiftsbeskrivning();
}
