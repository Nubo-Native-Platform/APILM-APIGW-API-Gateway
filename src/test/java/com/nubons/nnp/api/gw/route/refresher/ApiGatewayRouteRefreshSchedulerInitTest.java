package com.nubons.nnp.api.gw.route.refresher;

import com.nubons.nnp.api.gw.service.client.to.HeartBeatResp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("UnitTestCode-ApiGatewayRouteRefreshSchedulerInit")
class ApiGatewayRouteRefreshSchedulerInitTest {

    @AfterEach
    void tearDown() throws Exception {
        // Reset the static field so tests don't bleed into each other or later suites
        Field f = ApiGatewayRouteRefreshScheduler.class.getDeclaredField("lastRunTS");
        f.setAccessible(true);
        f.setLong(null, 0L);
    }

    @Test
    void initPrimesLastRunTSFromServiceResponse() throws Exception {
        long expectedTs = 1700000000000L;

        // Stub the scheduler so its webClient call returns a HeartBeatResp with lastUpdated=expectedTs
        ApiGatewayRouteRefreshSchedulerStub stub = new ApiGatewayRouteRefreshSchedulerStub(expectedTs);

        // Before init: lastRunTS is 0
        assertEquals(0L, getLastRunTS(), "lastRunTS should start at 0");

        stub.init();

        // After init: lastRunTS equals the service's lastUpdated
        assertEquals(expectedTs, getLastRunTS(), "lastRunTS should be primed to the service's lastUpdated");
    }

    @Test
    void initFallsBackToZeroIfServiceUnreachable() throws Exception {
        // A stub that simulates the service being unreachable (throws)
        ApiGatewayRouteRefreshSchedulerStub stub = new ApiGatewayRouteRefreshSchedulerStub(-1L);

        assertEquals(0L, getLastRunTS());

        // init() catches the exception, logs, and leaves lastRunTS at 0
        stub.init();

        assertEquals(0L, getLastRunTS(), "lastRunTS should stay 0 if init fails");
    }

    /**
     * Read the static lastRunTS field reflectively.
     */
    private long getLastRunTS() throws Exception {
        Field f = ApiGatewayRouteRefreshScheduler.class.getDeclaredField("lastRunTS");
        f.setAccessible(true);
        return f.getLong(null);
    }

    /**
     * Subclass that overrides the service call so we don't need a live HTTP server.
     * If fakeTs >= 0, returns a HeartBeatResp with that lastUpdated.
     * If fakeTs < 0, throws to simulate an unreachable service.
     */
    static class ApiGatewayRouteRefreshSchedulerStub extends ApiGatewayRouteRefreshScheduler {
        private final long fakeTs;

        ApiGatewayRouteRefreshSchedulerStub(long fakeTs) {
            this.fakeTs = fakeTs;
        }

        @Override
        protected HeartBeatResp fetchHeartBeat(long time) {
            if (fakeTs < 0) {
                throw new RuntimeException("simulated unreachable service");
            }
            HeartBeatResp resp = new HeartBeatResp();
            resp.setLastUpdated(fakeTs);
            return resp;
        }
    }
}
