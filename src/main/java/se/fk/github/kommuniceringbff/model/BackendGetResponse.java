package se.fk.github.kommuniceringbff.model;

import se.fk.rimfrost.regel.kommunicering.jaxrsspec.controllers.generatedsource.model.GetDataResponse;

// The regeltjänst wraps its data together with informationsaccess data (FRKOMM-FR-08.1)
public record BackendGetResponse(GetDataResponse data,BackendInformationsaccess informationsaccess){}
