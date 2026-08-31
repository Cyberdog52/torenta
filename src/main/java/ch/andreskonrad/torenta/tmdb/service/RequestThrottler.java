package ch.andreskonrad.torenta.tmdb.service;

import java.util.ArrayList;
import java.util.function.LongSupplier;

public class RequestThrottler {

    private final int maxRequests;
    private final int timeSpanForMaxRequestsInMillis;
    private final LongSupplier currentTimeMillis;
    private final Sleeper sleeper;

    public RequestThrottler(int maxRequests, int timeSpanForMaxRequestsInMillis) {
        this(maxRequests, timeSpanForMaxRequestsInMillis, System::currentTimeMillis, Thread::sleep);
    }

    RequestThrottler(
            int maxRequests,
            int timeSpanForMaxRequestsInMillis,
            LongSupplier currentTimeMillis,
            Sleeper sleeper
    ) {
        this.maxRequests = maxRequests;
        this.timeSpanForMaxRequestsInMillis = timeSpanForMaxRequestsInMillis;
        this.currentTimeMillis = currentTimeMillis;
        this.sleeper = sleeper;
    }

    private final ArrayList<Long> requestTimes = new ArrayList<>();

    public synchronized void throttle() throws InterruptedException {
        boolean gotThrough = false;
        do {
            this.clearOldRequestTimes();
            if (requestTimes.size() < maxRequests) {
                requestTimes.add(currentTimeMillis.getAsLong());
                gotThrough = true;
            } else {
                sleeper.sleep(Math.max(timeSpanForMaxRequestsInMillis / maxRequests, 0));
            }
        } while (!gotThrough);
    }

    private void clearOldRequestTimes() {
        long currentTime = currentTimeMillis.getAsLong();
        requestTimes.removeIf(requestTime -> requestTime <= currentTime - timeSpanForMaxRequestsInMillis);
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
