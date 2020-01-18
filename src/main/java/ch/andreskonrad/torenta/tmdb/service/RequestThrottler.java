package ch.andreskonrad.torenta.tmdb.service;

import java.util.ArrayList;

public class RequestThrottler {

    private final int maxRequests;
    private final int timeSpanForMaxRequestsInMillis;

    public RequestThrottler(int maxRequests, int timeSpanForMaxRequestsInMillis) {
        this.maxRequests = maxRequests;
        this.timeSpanForMaxRequestsInMillis = timeSpanForMaxRequestsInMillis;
    }

    private ArrayList<Long> requestTimes = new ArrayList();

    public synchronized void throttle() throws InterruptedException {
        boolean gotThrough = false;
        do {
            this.clearOldRequestTimes();
            if (requestTimes.size() < maxRequests) {
                requestTimes.add(System.currentTimeMillis());
                gotThrough = true;
            } else {
                Thread.sleep(Math.max(timeSpanForMaxRequestsInMillis / maxRequests, 0));
            }
        } while (!gotThrough);
    }

    private void clearOldRequestTimes() {
        long currentTime = System.currentTimeMillis();
        requestTimes.removeIf(requestTime -> requestTime < currentTime - timeSpanForMaxRequestsInMillis);
    }
}
