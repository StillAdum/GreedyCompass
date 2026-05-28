package com.adum.greedyCompass;

import java.util.UUID;

public class TrackingSession {
    private final UUID trackerUuid;
    private final UUID targetUuid;
    private int remainingSeconds;
    private String lastDimension;

    public TrackingSession(UUID trackerUuid, UUID targetUuid, int remainingSeconds, String lastDimension) {
        this.trackerUuid = trackerUuid;
        this.targetUuid = targetUuid;
        this.remainingSeconds = remainingSeconds;
        this.lastDimension = lastDimension;
    }

    public UUID getTrackerUuid() { return trackerUuid; }
    public UUID getTargetUuid() { return targetUuid; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int remainingSeconds) { this.remainingSeconds = remainingSeconds; }
    public String getLastDimension() { return lastDimension; }
    public void setLastDimension(String lastDimension) { this.lastDimension = lastDimension; }
}