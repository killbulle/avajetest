package com.example.watchdog;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Configuration for the watchdog monitor. Values can be provided through {@code application.yml}
 * or environment variables.
 */
@ConfigurationProperties("watchdog")
@Introspected
public class WatchdogConfigurationProperties {

  /** PID of the target JVM to monitor. */
  @NotNull
  private Long targetPid;

  /** Delay between two health evaluations. */
  @NotNull
  private Duration pollInterval = Duration.ofSeconds(5);

  /** Maximum allowed duration without any CPU activity from the monitored process. */
  @NotNull
  private Duration cpuIdleTimeout = Duration.ofSeconds(30);

  /** Configuration for /proc interactions. */
  @NotNull
  private ProcfsConfiguration procfs = new ProcfsConfiguration();

  /** Configuration for the attach-based probe. */
  @NotNull
  private AttachConfiguration attach = new AttachConfiguration();

  public Long getTargetPid() {
    return targetPid;
  }

  public void setTargetPid(Long targetPid) {
    this.targetPid = targetPid;
  }

  public Duration getPollInterval() {
    return pollInterval;
  }

  public void setPollInterval(Duration pollInterval) {
    this.pollInterval = pollInterval;
  }

  public Duration getCpuIdleTimeout() {
    return cpuIdleTimeout;
  }

  public void setCpuIdleTimeout(Duration cpuIdleTimeout) {
    this.cpuIdleTimeout = cpuIdleTimeout;
  }

  public ProcfsConfiguration getProcfs() {
    return procfs;
  }

  public void setProcfs(ProcfsConfiguration procfs) {
    this.procfs = procfs;
  }

  public AttachConfiguration getAttach() {
    return attach;
  }

  public void setAttach(AttachConfiguration attach) {
    this.attach = attach;
  }

  @Introspected
  @ConfigurationProperties("procfs")
  public static final class ProcfsConfiguration {

    /** Root directory of procfs. Default is {@code /proc}. */
    @NotNull
    private Path root = Path.of("/proc");

    /** Number of clock ticks per second, used to interpret CPU jiffies. */
    @Min(1)
    private long ticksPerSecond = 100;

    public Path getRoot() {
      return root;
    }

    public void setRoot(Path root) {
      this.root = root;
    }

    public long getTicksPerSecond() {
      return ticksPerSecond;
    }

    public void setTicksPerSecond(long ticksPerSecond) {
      this.ticksPerSecond = ticksPerSecond;
    }
  }

  @Introspected
  @ConfigurationProperties("attach")
  public static final class AttachConfiguration {

    /** Whether to perform the attach-based probe. */
    private boolean enabled = true;

    /** Maximum duration allowed for an attach attempt. */
    @NotNull
    private Duration timeout = Duration.ofSeconds(5);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }
}
