package se.fk.github.kommuniceringbff.model;

import jakarta.validation.constraints.NotNull;

public record UppgiftskommentarRequest(@NotNull String kommentar){}
