package ch.andreskonrad.torenta.tmdb.service;

import org.junit.Test;
import org.springframework.util.StopWatch;

import static org.junit.Assert.assertTrue;

public class RequestThrottlerTest {

    @Test
    public void requestThrottle_oneRequestsPerSecond_throttled() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RequestThrottler requestThrottler = new RequestThrottler(1, 100);
        requestThrottler.throttle();
        requestThrottler.throttle();
        requestThrottler.throttle();

        stopWatch.stop();
        assertTrue(stopWatch.getLastTaskTimeMillis() > 200);
    }

    @Test
    public void requestThrottle_threeRequestsPerSecond_throttled() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RequestThrottler requestThrottler = new RequestThrottler(3, 100);
        requestThrottler.throttle();
        requestThrottler.throttle();
        requestThrottler.throttle();

        stopWatch.stop();
        assertTrue(stopWatch.getLastTaskTimeMillis() < 100);
    }

    @Test
    public void requestThrottle_waitToClear_notThrottled() throws InterruptedException {
        RequestThrottler requestThrottler = new RequestThrottler(1, 100);

        requestThrottler.throttle();
        Thread.sleep(100);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        requestThrottler.throttle();

        stopWatch.stop();
        assertTrue(stopWatch.getLastTaskTimeMillis() < 100);
    }
}
