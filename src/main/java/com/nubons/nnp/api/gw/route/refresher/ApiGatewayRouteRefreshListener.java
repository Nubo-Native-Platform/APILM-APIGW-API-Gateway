/*
 * package com.nubons.nnp.api.gw.route.refresher;
 * 
 * import static
 * com.nubons.nnp.api.abs.constants.APIConstants.REFRESH_ROUTE_EVENT; import
 * static com.nubons.nnp.api.abs.constants.APIConstants.REFRESH_TOPIC;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.beans.factory.annotation.Value; import
 * org.springframework.http.ResponseEntity; import
 * org.springframework.jms.annotation.JmsListener; import
 * org.springframework.stereotype.Component; import
 * org.springframework.web.client.RestTemplate;
 * 
 * import lombok.extern.slf4j.Slf4j;
 * 
 * @Component
 * 
 * @Slf4j public class ApiGatewayRouteRefreshListener {
 * 
 * private static final String LOG_STATEMENT_ROUTE_REFRESH =
 * "Route Refresh event was triggered, actuator endpoint has been called which has been returned with status: {}"
 * ;
 * 
 * @Value("${gateway.base.url}") private String gatewayUrl;
 * 
 * @Autowired private RestTemplate restTemplate;
 * 
 * @JmsListener(destination = REFRESH_TOPIC, containerFactory =
 * "containerFactory") public void readMsg(String event) { try {
 * 
 * if (REFRESH_ROUTE_EVENT.equalsIgnoreCase(event)) {
 * log.info("Received Event in Gateway {}", event); ResponseEntity<String>
 * toResp = restTemplate.postForEntity(gatewayUrl + "/actuator/gateway/refresh",
 * null, String.class); log.info(LOG_STATEMENT_ROUTE_REFRESH,
 * toResp.getStatusCode()); } else { log.error("Received Invalid Event Type {}",
 * event); } } catch (Exception ex) {
 * log.error("Exception caught while Refreshing Gateway routes", ex); } } }
 */