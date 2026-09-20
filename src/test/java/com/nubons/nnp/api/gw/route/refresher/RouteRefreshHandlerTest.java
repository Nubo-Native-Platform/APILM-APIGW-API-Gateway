package com.nubons.nnp.api.gw.route.refresher;

import com.nubons.nnp.api.gw.route.repo.GwServiceRouteDefinationRepository.GwStateModifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("UnitTestCode-RouteRefreshHandler")
class RouteRefreshHandlerTest {

    private GwStateModifier gwStateModifier;
    private ApplicationEventPublisher eventPublisher;
    private ServerRequest serverRequest;

    @BeforeEach
    void setup() {
        gwStateModifier = mock(GwStateModifier.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        serverRequest = mock(ServerRequest.class);
    }

    @Test
    void resetsStateAndPublishesRefreshRoutesEvent() throws Exception {
        RouteRefreshHandler handler = newHandler();

        StepVerifier.create(handler.refreshRoutes(serverRequest))
                .expectNextMatches(resp -> resp.statusCode().equals(HttpStatus.OK))
                .expectComplete()
                .verify();

        verify(gwStateModifier, times(1)).resetGwState();
        verify(eventPublisher, times(1)).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void stillReturnsOkWhenEventPublishingFails() throws Exception {
        // The handler must still return 200 (it logs the failure but doesn't
        // propagate it to the caller), so the caller can retry.
        doThrow(new RuntimeException("simulated listener failure")).when(eventPublisher)
                .publishEvent(any(RefreshRoutesEvent.class));
        RouteRefreshHandler handler = newHandler();

        StepVerifier.create(handler.refreshRoutes(serverRequest))
                .expectNextMatches(resp -> resp.statusCode().equals(HttpStatus.OK))
                .expectComplete()
                .verify();

        verify(gwStateModifier, times(1)).resetGwState();
    }

    private RouteRefreshHandler newHandler() throws Exception {
        RouteRefreshHandler handler = new RouteRefreshHandler();
        Field modField = RouteRefreshHandler.class.getDeclaredField("gwStateModifier");
        modField.setAccessible(true);
        modField.set(handler, gwStateModifier);
        Field pubField = RouteRefreshHandler.class.getDeclaredField("eventPublisher");
        pubField.setAccessible(true);
        pubField.set(handler, eventPublisher);
        return handler;
    }
}
