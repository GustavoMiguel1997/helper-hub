package io.helperhub.api.common.support;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Fixed windows with bounded memory. Capacity exhaustion never resets existing clients. */
public final class ClientRateLimiter {
  private static final long WINDOW_NANOS = java.time.Duration.ofMinutes(1).toNanos();
  private final Map<String, Window> clients = new HashMap<>();
  private final int maxClients;
  private final LongSupplier clock;
  private long nextCleanup;

  public ClientRateLimiter() { this(100_000, System::nanoTime); }
  public ClientRateLimiter(int maxClients, LongSupplier clock) {
    this.maxClients = maxClients;
    this.clock = clock;
    this.nextCleanup = clock.getAsLong() + WINDOW_NANOS;
  }

  public synchronized boolean tryConsume(String client, int limit) {
    long now = clock.getAsLong();
    if (now - nextCleanup >= 0) {
      clients.values().removeIf(window -> now - window.started >= WINDOW_NANOS);
      nextCleanup = now + WINDOW_NANOS;
    }
    Window window = clients.get(client);
    if (window == null) {
      if (clients.size() >= maxClients) return false;
      window = new Window(now);
      clients.put(client, window);
    } else if (now - window.started >= WINDOW_NANOS) {
      window.started = now;
      window.used = 0;
    }
    if (window.used >= Math.max(1, limit)) return false;
    window.used++;
    return true;
  }

  private static final class Window {
    long started;
    int used;
    Window(long started) { this.started = started; }
  }
}
