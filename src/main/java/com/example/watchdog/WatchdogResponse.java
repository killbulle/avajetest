package com.example.watchdog;

import io.micronaut.core.annotation.Introspected;
import java.time.Instant;
import java.util.Map;

/**
 * DTO returned by the HTTP probes.
 */
@Introspected
public record WatchdogResponse(boolean healthy, Instant timestamp, String message, Map<String, Object> details) {

  public WatchdogResponse(WatchdogHealthSnapshot snapshot) {
    this(snapshot.healthy(), snapshot.timestamp(), snapshot.message(), snapshot.details());
  }
}
