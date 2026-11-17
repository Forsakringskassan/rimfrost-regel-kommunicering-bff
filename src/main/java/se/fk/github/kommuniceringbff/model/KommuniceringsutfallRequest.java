package se.fk.github.kommuniceringbff.model;

import jakarta.validation.constraints.NotNull;
import se.fk.rimfrost.framework.regel.kommuniceringsutfall.jaxrsspec.controllers.generatedsource.model.Kommuniceringsutfall;

public record KommuniceringsutfallRequest(@NotNull Kommuniceringsutfall utfall){}
