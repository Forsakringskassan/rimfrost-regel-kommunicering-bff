package se.fk.github.kommuniceringbff.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PatchTaskRequest(@NotEmpty List<@NotNull @Valid UpdateErsattning>ersattningar,@NotNull String motivering){public PatchTaskRequest{
// ArrayList rather than List.copyOf so null elements reach bean validation (400) instead of throwing here
ersattningar=ersattningar==null?null:Collections.unmodifiableList(new ArrayList<>(ersattningar));}}
