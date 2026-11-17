package se.fk.github.kommuniceringbff.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import se.fk.rimfrost.framework.regel.kommuniceringsutfall.jaxrsspec.controllers.generatedsource.model.Kommuniceringsutfall;

public record KommuniceringsutfallRequest(@NotBlank @Pattern(regexp=HandlaggningIdFormat.UUID)String handlaggningId,@NotNull Kommuniceringsutfall utfall){}
