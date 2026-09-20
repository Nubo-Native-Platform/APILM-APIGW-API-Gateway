package com.nubons.nnp.api.gw.route.refresher;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.nubons.nnp.api.gw.route.repo.GwServiceRouteDefinationRepository;
import com.nubons.nnp.api.gw.service.client.to.HeartBeatResp;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApiGatewayRouteRefreshScheduler {
	
	@Qualifier("webClientGwServ")
	@Autowired
	WebClient webClient;
	
	@Value("${gateway.base.url}")
	private String gatewayUrl;
	
	@Autowired
	private GwServiceRouteDefinationRepository.GwStateModifier gwStateModifier;

	private static long lastRunTS = 0;
	
	private static final String LOG_STATEMENT_ROUTE_REFRESH = "Route Refresh event was triggered, actuator endpoint has been called which has been returned with status: {}";

	@PostConstruct
	public void init() {
		// Prime the baseline at startup so the first scheduled poll can detect
		// changes that happened since boot. GET /refresh?time=0 returns the
		// current latest updated_on; we store it as the baseline without
		// triggering a refresh (lastRunTS != 0 satisfies the guard, and the
		// service returns refresh=false on the next poll unless the DB
		// advanced past this value).
		try {
			HeartBeatResp resp = fetchHeartBeat(0);
			if (resp != null) {
				lastRunTS = resp.getLastUpdated();
				log.info("Scheduler baseline primed at startup: {}", lastRunTS);
			} else {
				log.debug("Scheduler baseline not primed: service returned null response; leaving lastRunTS at 0");
			}
		} catch (Exception e) {
			log.warn("Could not prime scheduler baseline at startup; first poll will prime it: {}", e.getMessage());
		}
	}

	/**
	 * Calls the gateway-service GET /refresh?time=<lastRunTS> and returns the
	 * heartbeat response. Extracted as a protected method so unit tests can
	 * override it without a live HTTP server / gateway-service.
	 */
	protected HeartBeatResp fetchHeartBeat(long time) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/refresh").queryParam("time", time).build())
				.retrieve().bodyToMono(HeartBeatResp.class)
				.timeout(Duration.ofSeconds(10))
				.block();
	}

	@Scheduled(timeUnit = TimeUnit.MINUTES, fixedRateString = "${task.expCheck.interval}")
	public void doRefreshHearbeat() {
		//call webservice to check refresh required
		log.info("Sending heartbeat to check refresh required after " + lastRunTS + " ?");
		HeartBeatResp heartBeatResp = fetchHeartBeat(lastRunTS);
		if (lastRunTS != 0 && Objects.nonNull(heartBeatResp) && heartBeatResp.isRefresh()) {

			gwStateModifier.resetGwState();

			ResponseEntity<String> resp = WebClient.create(gatewayUrl).post().uri(ub -> ub.path("/actuator/gateway/refresh").build()).retrieve().toEntity(String.class).block();
			if (Objects.nonNull(resp) && HttpStatus.OK.equals(resp.getStatusCode())) {
				log.info(LOG_STATEMENT_ROUTE_REFRESH, resp.getStatusCode());
			} else {
				if (Objects.nonNull(resp))
					log.error("Error while Refreshing Gateway routes", resp.getStatusCode());
				else
					log.error("Error while Refreshing Gateway routes");
			}

		} else {
			if (Objects.nonNull(heartBeatResp))
				log.info("Refresh not required as value of boolean refresh returned is " + heartBeatResp.isRefresh());
			else
				log.info("heartBeatResp could not be retrieved");
		}
		if (Objects.nonNull(heartBeatResp)) {
			lastRunTS = heartBeatResp.getLastUpdated();
		}
	}
	
}
