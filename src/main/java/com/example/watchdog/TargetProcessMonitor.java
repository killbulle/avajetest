package com.example.watchdog;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Periodically evaluates the health of the target JVM process.
 */
@Singleton
public class TargetProcessMonitor implements AutoCloseable {

  private final long pid;
  private final WatchdogConfigurationProperties configuration;
  private final ProcessCpuProbe cpuProbe;
  private final AttachProbe attachProbe;
  private final ScheduledExecutorService executor;
  private final AtomicReference<WatchdogHealthSnapshot> snapshot;
  private final Clock clock;

  public TargetProcessMonitor(
      WatchdogConfigurationProperties configuration,
      ProcessCpuProbe cpuProbe,
      AttachProbe attachProbe) {
    this.configuration = configuration;
    this.cpuProbe = cpuProbe;
    this.attachProbe = attachProbe;
    this.pid = configuration.getTargetPid();
    this.executor =
        Executors.newSingleThreadScheduledExecutor(r -> {
          Thread thread = new Thread(r, "watchdog-monitor");
          thread.setDaemon(true);
          return thread;
        });
    this.snapshot = new AtomicReference<>(WatchdogHealthSnapshot.bootstrapping());
    this.clock = Clock.systemUTC();
  }

  @PostConstruct
  void start() {
    long interval = Math.max(1, configuration.getPollInterval().toMillis());
    executor.scheduleWithFixedDelay(this::evaluate, 0, interval, TimeUnit.MILLISECONDS);
  }

  public WatchdogHealthSnapshot currentSnapshot() {
    return snapshot.get();
  }

  private void evaluate() {
    Instant now = clock.instant();
    Map<String, Object> details = new HashMap<>();

    ProcessHandle processHandle = ProcessHandle.of(pid).orElse(null);
    if (processHandle == null || !processHandle.isAlive()) {
      snapshot.set(WatchdogHealthSnapshot.down(now, "process not running", Map.of("pid", pid)));
      return;
    }

    details.put("pid", pid);
    details.put("uptimeMillis", processHandle.info().totalCpuDuration().map(Duration::toMillis).orElse(0L));

    ProcessCpuProbe.CpuProbeResult cpuResult = cpuProbe.evaluate(now);
    details.put("cpu", cpuResult.details());
    details.put("cpuMessage", cpuResult.message());

    AttachProbe.AttachProbeResult attachResult = attachProbe.evaluate();
    details.put("attach", attachResult.details());
    details.put("attachMessage", attachResult.message());

    boolean healthy = cpuResult.healthy() && attachResult.healthy();
    String message;
    if (healthy) {
      message = "watchdog ok";
    } else if (!cpuResult.healthy()) {
      message = cpuResult.message();
    } else {
      message = attachResult.message();
    }

    snapshot.set(new WatchdogHealthSnapshot(healthy, now, message, Map.copyOf(details)));
  }

  @Override
  @PreDestroy
  public void close() {
    executor.shutdownNow();
  }
}
