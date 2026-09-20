package com.nubons.nnp.api.gw.service.client.to;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class ApiRouteTO extends AbstractBaseTO implements Serializable {

	private static final long serialVersionUID = 3578454999577694327L;

	private String apiRouteId;
	private String name;
	private JsonNode filter;
	private JsonNode predicate;
	private String url;



}