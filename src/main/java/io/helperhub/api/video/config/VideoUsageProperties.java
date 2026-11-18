package io.helperhub.api.video.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "helperhub.video")
public class VideoUsageProperties {

  /**
   * Limite de requisições por minuto e por cliente (IP da conexão) nos endpoints de vídeo.
   */
  private int maxRequestsPerMinute = 20;

  private boolean rateLimitEnabled = true;

  /**
   * Comando utilizado para executar o yt-dlp (ex.: yt-dlp, yt-dlp.exe ou caminho absoluto).
   */
  private String ytDlpCommand = "yt-dlp";

  /**
   * Tempo máximo de execução do download/conversão em segundos.
   */
  private long maxDownloadDurationSeconds = 900;
  private long maxDownloadBytes = 250L * 1024 * 1024;
  private long maxVideoDurationSeconds = 1800;
  private long maxCompressDurationSeconds = 300;
  private long maxCompressProcessingSeconds = 180;
  private long maxCompressOutputBytes = 50L * 1024 * 1024;
  public long getMaxCompressDurationSeconds() { return maxCompressDurationSeconds; }
  public void setMaxCompressDurationSeconds(long value) { maxCompressDurationSeconds = Math.max(1,value); }
  public long getMaxCompressProcessingSeconds() { return maxCompressProcessingSeconds; }
  public void setMaxCompressProcessingSeconds(long value) { maxCompressProcessingSeconds = Math.max(1,value); }
  public long getMaxCompressOutputBytes() { return maxCompressOutputBytes; }
  public void setMaxCompressOutputBytes(long value) { maxCompressOutputBytes = Math.max(1,value); }

  public long getMaxDownloadBytes() { return maxDownloadBytes; }
  public void setMaxDownloadBytes(long value) { maxDownloadBytes = Math.max(1, value); }
  public long getMaxVideoDurationSeconds() { return maxVideoDurationSeconds; }
  public void setMaxVideoDurationSeconds(long value) { maxVideoDurationSeconds = Math.max(1, value); }

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

  public String getYtDlpCommand() {
    return ytDlpCommand;
  }

  public void setYtDlpCommand(String ytDlpCommand) {
    this.ytDlpCommand = ytDlpCommand;
  }

  public long getMaxDownloadDurationSeconds() {
    return maxDownloadDurationSeconds;
  }

  public void setMaxDownloadDurationSeconds(long maxDownloadDurationSeconds) {
    this.maxDownloadDurationSeconds = maxDownloadDurationSeconds;
  }
}
