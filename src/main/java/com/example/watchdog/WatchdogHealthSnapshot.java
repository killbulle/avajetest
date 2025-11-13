package com.example.watchdog;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable representation of the watchdog health.
 */
public record WatchdogHealthSnapshot(
    boolean healthy, Instant timestamp, String message, Map<String, Object> details) {

  public static WatchdogHealthSnapshot bootstrapping() {
    return new WatchdogHealthSnapshot(false, Instant.EPOCH, "starting", Map.of());
  }

  public static WatchdogHealthSnapshot down(Instant timestamp, String message, Map<String, Object> details) {
    return new WatchdogHealthSnapshot(false, timestamp, message, Map.copyOf(details));
  }
}
