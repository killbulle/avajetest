package com.example.watchdog;

import io.micronaut.runtime.Micronaut;

/**
 * Entry point for the watchdog application. The application exposes HTTP probes that report on
 * the health of a target JVM process running in the same host namespace.
 */
public final class WatchdogApplication {

  private WatchdogApplication() {
  }

  public static void main(String[] args) {
    Micronaut.run(WatchdogApplication.class, args);
  }
}
