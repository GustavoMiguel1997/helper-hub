package io.helperhub.api.audio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "helperhub.audio")
public class AudioUsageProperties {

  /**
   * Duração máxima permitida para um único trecho de corte (endMs - startMs).
   */
  private long maxCutDurationMs = 30L * 60 * 1000;

  /**
   * Limite de requisições por minuto e por cliente (IP da conexão) nos endpoints de áudio.
   */
  private int maxRequestsPerMinute = 60;

  private boolean rateLimitEnabled = true;
  private long maxProcessingSeconds = 120;
  private long maxEditDurationMs = 30L * 60 * 1000;
  public long getMaxEditDurationMs() { return maxEditDurationMs; }
  public void setMaxEditDurationMs(long value) { maxEditDurationMs = Math.max(1, value); }
  public long getMaxProcessingSeconds() { return maxProcessingSeconds; }
  public void setMaxProcessingSeconds(long value) { maxProcessingSeconds = Math.max(1, value); }

  public long getMaxCutDurationMs() {
    return maxCutDurationMs;
  }

  public void setMaxCutDurationMs(long maxCutDurationMs) {
    this.maxCutDurationMs = maxCutDurationMs;
  }

  public int getMaxRequestsPerMinute() {
    return maxRequestsPerMinute;
  }

  public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
    this.maxRequestsPerMinute = maxRequestsPerMinute;
  }

  public boolean isRateLimitEnabled() {
    return rateLimitEnabled;
  }

  public void setRateLimitEnabled(boolean rateLimitEnabled) {
    this.rateLimitEnabled = rateLimitEnabled;
  }
}
