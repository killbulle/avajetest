package com.example.watchdog;

import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads CPU information from {@code /proc/<pid>/stat} to ensure the monitored JVM is still active.
 */
@Singleton
public class ProcessCpuProbe {

  private final long pid;
  private final Path statFile;
  private final long ticksPerSecond;
  private final Duration idleTimeout;
  private final AtomicReference<ProcessCpuSample> lastSample = new AtomicReference<>();
  private final AtomicReference<Instant> lastActivity = new AtomicReference<>();

  public ProcessCpuProbe(WatchdogConfigurationProperties configuration) {
    this.pid = configuration.getTargetPid();
    this.ticksPerSecond = configuration.getProcfs().getTicksPerSecond();
    this.idleTimeout = configuration.getCpuIdleTimeout();
    this.statFile = configuration.getProcfs().getRoot().resolve(Long.toString(pid)).resolve("stat");
  }

  public CpuProbeResult evaluate(Instant now) {
    try {
      ProcessCpuSample sample = readSample(now);
      if (sample == null) {
        return CpuProbeResult.failure("unable to read /proc data for pid " + pid);
      }

      ProcessCpuSample previous = lastSample.getAndSet(sample);
      Instant activityMoment = lastActivity.updateAndGet(existing -> updateLastActivity(existing, sample, previous));
      Duration idle = activityMoment == null ? Duration.ZERO : Duration.between(activityMoment, now);

      if (idle.compareTo(idleTimeout) > 0) {
        return CpuProbeResult.failure("no cpu activity for " + idle);
      }

      Map<String, Object> details = new HashMap<>();
      details.put("totalJiffies", sample.totalJiffies());
      if (previous != null) {
        details.put("deltaJiffies", Math.max(0, sample.totalJiffies() - previous.totalJiffies()));
      }
      details.put("ticksPerSecond", ticksPerSecond);
      details.put("statFile", statFile.toString());

      return CpuProbeResult.success(details);
    } catch (IOException e) {
      return CpuProbeResult.failure("failed to read /proc stat: " + e.getMessage());
    }
  }

  private Instant updateLastActivity(Instant currentActivity, ProcessCpuSample sample, ProcessCpuSample previous) {
    if (previous == null || sample.totalJiffies() > previous.totalJiffies()) {
      return sample.timestamp();
    }
    return currentActivity;
  }

  private ProcessCpuSample readSample(Instant timestamp) throws IOException {
    if (!Files.isRegularFile(statFile)) {
      return null;
    }
    String statContent = Files.readString(statFile, StandardCharsets.US_ASCII).trim();
    if (StringUtils.isEmpty(statContent)) {
      return null;
    }

    int commandEnd = statContent.lastIndexOf(')');
    if (commandEnd <= 0 || commandEnd + 2 >= statContent.length()) {
      return null;
    }
    String after = statContent.substring(commandEnd + 2);
    String[] fields = after.trim().split("\\s+");
    if (fields.length < 15) {
      return null;
    }

    long userTicks = parseLong(fields[11]);
    long systemTicks = parseLong(fields[12]);
    long total = userTicks + systemTicks;
    return new ProcessCpuSample(total, timestamp);
  }

  private long parseLong(String value) {
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  public record CpuProbeResult(boolean healthy, String message, Map<String, Object> details) {

    public static CpuProbeResult success(Map<String, Object> details) {
      return new CpuProbeResult(true, "cpu probe ok", Map.copyOf(details));
    }

    public static CpuProbeResult failure(String message) {
      return new CpuProbeResult(false, message, Map.of());
    }
  }

  private record ProcessCpuSample(long totalJiffies, Instant timestamp) {}
}
