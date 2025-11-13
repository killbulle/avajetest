package com.example.watchdog;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.MediaType;
import jakarta.inject.Inject;

/**
 * Exposes HTTP endpoints that surface the watchdog health state.
 */
@Controller
public class WatchdogController {

  private final TargetProcessMonitor monitor;

  @Inject
  public WatchdogController(TargetProcessMonitor monitor) {
    this.monitor = monitor;
  }

  @Get("/health/liveness")
  @Produces(MediaType.APPLICATION_JSON)
  public HttpResponse<WatchdogResponse> liveness() {
    WatchdogHealthSnapshot snapshot = monitor.currentSnapshot();
    HttpStatus status = snapshot.healthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return HttpResponse.status(status).body(new WatchdogResponse(snapshot));
  }
}
