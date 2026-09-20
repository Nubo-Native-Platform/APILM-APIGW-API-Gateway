package com.nubons.nnp.api.gw.route.refresher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.nubons.nnp.api.gw.route.repo.GwServiceRouteDefinationRepository.GwStateModifier;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class RouteRefreshHandler {

    @Autowired
    private GwStateModifier gwStateModifier;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Mono<ServerResponse> refreshRoutes(ServerRequest request) {
        log.info("Manual route refresh triggered");
        gwStateModifier.resetGwState();
        try {
            eventPublisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("Manual route refresh completed, RefreshRoutesEvent published");
        } catch (Exception e) {
            log.error("Manual route refresh: publishing RefreshRoutesEvent failed: {}", e.getMessage());
        }
        return ServerResponse.ok().build();
    }
}
