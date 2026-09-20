package com.nubons.nnp.api.gw.service.client.to;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonInclude(Include.NON_NULL)
public abstract class AbstractBaseTO implements Serializable {

	private static final long serialVersionUID = 8949719762906990408L;

	private static ObjectMapper mapper = new ObjectMapper();

	public static JsonNode toJSON(String jsonStr) {
		if (jsonStr != null && jsonStr.trim().length() > 0) {
			try {
				return mapper.readValue(jsonStr, JsonNode.class);
			} catch (JsonProcessingException e) {
				log.error("Error while parsing json ", e);
			}
		}
		return null;
	}

	public static String fromJSON(JsonNode jsonObj) {
		if (jsonObj != null) {
			try {
				return mapper.writeValueAsString(jsonObj);
			} catch (JsonProcessingException e) {
				log.error("Error while parsing json ", e);
			}
		}
		return null;
	}

}
