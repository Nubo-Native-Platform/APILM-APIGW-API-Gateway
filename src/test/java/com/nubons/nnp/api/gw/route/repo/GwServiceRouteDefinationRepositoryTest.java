package com.nubons.nnp.api.gw.route.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubons.nnp.api.gw.service.client.to.ApiRouteTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.Arrays;

@DisplayName("UnitTestCode-GwServiceRouteDefinationRepository")
class GwServiceRouteDefinationRepositoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PATH_PREDICATE = "[{\"name\":\"Path\",\"args\":[{\"patterns\":\"/good/**\"}]}]";

    private GwServiceRouteDefinationRepository repository;

    private static ApiRouteTO route(String id, String name, String url) {
        return route(id, name, url, PATH_PREDICATE, null);
    }

    private static ApiRouteTO route(String id, String name, String url, String predicateJson, String filterJson) {
        ApiRouteTO to = new ApiRouteTO();
        to.setApiRouteId(id);
        to.setName(name);
        to.setUrl(url);
        to.setPredicate(jsonNode(predicateJson));
        to.setFilter(jsonNode(filterJson));
        return to;
    }

    private static JsonNode jsonNode(String json) {
        try {
            return json == null ? null : MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("broken test fixture json", e);
        }
    }

    @BeforeEach
    void setup() throws Exception {
        repository = new GwServiceRouteDefinationRepository();
        Field objectMapperField = GwServiceRouteDefinationRepository.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(repository, MAPPER);
    }

    @Test
    void emitsAllHealthyRoutes() {
        ApiRouteTO good1 = route("good1", "Good One", "http://good1.example.com");
        ApiRouteTO good2 = route("good2", "Good Two", "http://good2.example.com");

        StepVerifier.create(repository.getRdFlux(Arrays.asList(good1, good2)))
                .expectNextMatches(rd -> "good1".equals(rd.getId()) && rd.getUri() != null)
                .expectNextMatches(rd -> "good2".equals(rd.getId()))
                .verifyComplete();
    }

    @Test
    void skipsRouteWithBrokenPredicateArgsAndKeepsHealthyOnes() {
        // "args" is a JSON object instead of the expected array of maps
        ApiRouteTO broken = route("broken", "Broken", "http://broken.example.com",
                "[{\"name\":\"Path\",\"args\":{\"patterns\":\"/broken/**\"}}]", null);
        ApiRouteTO good = route("good", "Good", "http://good.example.com");

        StepVerifier.create(repository.getRdFlux(Arrays.asList(broken, good)))
                .expectNextMatches(rd -> "good".equals(rd.getId()))
                .verifyComplete();
    }

    @Test
    void skipsRouteWithBrokenFilterArgsAndKeepsHealthyOnes() {
        // "args" is a JSON object instead of the expected array of maps
        ApiRouteTO broken = route("broken", "Broken", "http://broken.example.com", null,
                "[{\"name\":\"ApiGatewayRateLimitFilter\",\"args\":{\"replenishRate\":\"10\"}}]");
        ApiRouteTO good = route("good", "Good", "http://good.example.com");

        StepVerifier.create(repository.getRdFlux(Arrays.asList(broken, good)))
                .expectNextMatches(rd -> "good".equals(rd.getId()))
                .verifyComplete();
    }

    @Test
    void skipsRouteWithMalformedUrlAndKeepsHealthyOnes() {
        ApiRouteTO broken = route("broken", "Broken", "http://exa mple.com");
        ApiRouteTO good = route("good", "Good", "http://good.example.com");

        StepVerifier.create(repository.getRdFlux(Arrays.asList(broken, good)))
                .expectNextMatches(rd -> "good".equals(rd.getId()))
                .verifyComplete();
    }

    @Test
    void skipsRouteWithMissingUrlAndKeepsHealthyOnes() {
        ApiRouteTO broken = route("broken", "Broken", null);
        ApiRouteTO good = route("good", "Good", "http://good.example.com");

        StepVerifier.create(repository.getRdFlux(Arrays.asList(broken, good)))
                .expectNextMatches(rd -> "good".equals(rd.getId()))
                .verifyComplete();
    }

    @Test
    void skipsRouteWithUnparsablePredicateJsonAndKeepsHealthyOnes() {
        // predicate is a JSON object, not the expected array of predicate definitions
        ApiRouteTO broken = route("broken", "Broken", "http://broken.example.com", "{\"bad\":true}", null);
        ApiRouteTO good = route("good", "Good", "http://good.example.com");

        StepVerifier.create(repository.getRdFlux(Arrays.asList(broken, good)))
                .expectNextMatches(rd -> "good".equals(rd.getId()))
                .verifyComplete();
    }
}
