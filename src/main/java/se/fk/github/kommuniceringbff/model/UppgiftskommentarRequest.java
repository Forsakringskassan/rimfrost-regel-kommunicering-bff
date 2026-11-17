package se.fk.github.kommuniceringbff.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UppgiftskommentarRequest(@NotBlank @Pattern(regexp=HandlaggningIdFormat.UUID)String handlaggningId,@NotNull String kommentar){}
