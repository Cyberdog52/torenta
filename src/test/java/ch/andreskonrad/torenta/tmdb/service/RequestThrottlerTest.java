package ch.andreskonrad.torenta.tmdb.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestThrottlerTest {

    @Test
    void throttle_underLimit_doesNotSleep() throws InterruptedException {
        FakeTime fakeTime = new FakeTime();
        RequestThrottler throttler = throttler(3, 100, fakeTime);

        throttler.throttle();
        throttler.throttle();

        assertEquals(List.of(), fakeTime.sleepDurations);
    }

    @Test
    void throttle_atLimit_doesNotSleep() throws InterruptedException {
        FakeTime fakeTime = new FakeTime();
        RequestThrottler throttler = throttler(3, 100, fakeTime);

        throttler.throttle();
        throttler.throttle();
        throttler.throttle();

        assertEquals(List.of(), fakeTime.sleepDurations);
    }

    @Test
    void throttle_overLimit_sleepsUntilWindowExpires() throws InterruptedException {
        FakeTime fakeTime = new FakeTime();
        RequestThrottler throttler = throttler(2, 100, fakeTime);

        throttler.throttle();
        throttler.throttle();
        throttler.throttle();

        assertEquals(List.of(50L, 50L), fakeTime.sleepDurations);
        assertEquals(100, fakeTime.currentTimeMillis());
    }

    @Test
    void throttle_expiredWindow_doesNotSleep() throws InterruptedException {
        FakeTime fakeTime = new FakeTime();
        RequestThrottler throttler = throttler(1, 100, fakeTime);

        throttler.throttle();
        fakeTime.advance(100);
        throttler.throttle();

        assertEquals(List.of(), fakeTime.sleepDurations);
    }

    private RequestThrottler throttler(int maxRequests, int windowMillis, FakeTime fakeTime) {
        return new RequestThrottler(
                maxRequests,
                windowMillis,
                fakeTime::currentTimeMillis,
                fakeTime::sleep
        );
    }

    private static class FakeTime {

        private final List<Long> sleepDurations = new ArrayList<>();
        private long currentTimeMillis;

        long currentTimeMillis() {
            return currentTimeMillis;
        }

        void advance(long millis) {
            currentTimeMillis += millis;
        }

        void sleep(long millis) {
            sleepDurations.add(millis);
            advance(millis);
        }
    }
}
