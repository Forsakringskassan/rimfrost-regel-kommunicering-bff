package se.fk.github.kommuniceringbff.model;

import java.util.UUID;

public record BackendInformationsaccess(UUID uppgiftId,UUID regelId,String regelVersion,BackendAnvandare anvandare){}
