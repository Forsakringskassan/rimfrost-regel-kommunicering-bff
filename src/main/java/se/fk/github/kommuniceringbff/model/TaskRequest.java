package se.fk.github.kommuniceringbff.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TaskRequest(@NotBlank @Pattern(regexp=HandlaggningIdFormat.UUID)String handlaggningId){}
