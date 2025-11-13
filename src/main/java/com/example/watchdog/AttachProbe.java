package com.example.watchdog;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs a lightweight attach to the monitored JVM using the same mechanics as {@code jattach}.
 */
@Singleton
public class AttachProbe {

  private final long pid;
  private final boolean enabled;
  private final Duration timeout;
  private final ExecutorService executor;

  public AttachProbe(WatchdogConfigurationProperties configuration) {
    this.pid = configuration.getTargetPid();
    this.enabled = configuration.getAttach().isEnabled();
    this.timeout = configuration.getAttach().getTimeout();
    this.executor = Executors.newSingleThreadExecutor(new AttachThreadFactory());
  }

  public AttachProbeResult evaluate() {
    if (!enabled) {
      return AttachProbeResult.disabled();
    }

    Future<AttachProbeResult> future = executor.submit(new AttachTask());
    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      return AttachProbeResult.failure("attach timed out after " + timeout);
    } catch (ExecutionException e) {
      return AttachProbeResult.failure("attach failed: " + e.getCause().getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return AttachProbeResult.failure("attach interrupted");
    }
  }

  @PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }

  private class AttachTask implements Callable<AttachProbeResult> {

    @Override
    public AttachProbeResult call() throws Exception {
      try {
        VirtualMachine vm = VirtualMachine.attach(Long.toString(pid));
        try {
          Map<String, String> details = new HashMap<>();
          details.put("pid", Long.toString(pid));
          details.put("java.version", vm.getSystemProperties().getProperty("java.version", "unknown"));
          return AttachProbeResult.success(details);
        } finally {
          vm.detach();
        }
      } catch (AttachNotSupportedException e) {
        return AttachProbeResult.failure("attach not supported: " + e.getMessage());
      } catch (IOException e) {
        return AttachProbeResult.failure("attach io error: " + e.getMessage());
      }
    }
  }

  private static class AttachThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "watchdog-attach");
      thread.setDaemon(true);
      return thread;
    }
  }

  public record AttachProbeResult(boolean healthy, String message, Map<String, Object> details) {

    private static final AttachProbeResult DISABLED =
        new AttachProbeResult(true, "attach probe disabled", Map.of());

    public static AttachProbeResult disabled() {
      return DISABLED;
    }

    public static AttachProbeResult success(Map<String, String> details) {
      return new AttachProbeResult(true, "attach ok", Map.copyOf(details));
    }

    public static AttachProbeResult failure(String message) {
      return new AttachProbeResult(false, message, Map.of());
    }
  }
}
