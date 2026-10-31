package io.helperhub.api.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "helperhub.image")
public class ImageUsageProperties {

  /**
   * Chave da API remove.bg ({@code X-Api-Key}). Sem ela, o endpoint de remoção de fundo responde com
   * erro de configuração.
   */
  private String removeBgApiKey = "";

  private int maxRequestsPerMinute = 30;

  private boolean rateLimitEnabled = true;

  /**
   * Lado máximo (largura ou altura) permitido na saída do resize (pedido e resultado).
   */
  private int maxResizeOutputSide = 8192;
  private long maxInputPixels = 25_000_000;
  public long getMaxInputPixels() { return maxInputPixels; }
  public void setMaxInputPixels(long value) { maxInputPixels = Math.max(1, value); }

  /**
   * Qualidade JPEG de saída (0.0–1.0) quando o formato de saída for JPEG e o cliente não enviar jpegQuality.
   */
  private float defaultJpegOutputQuality = 0.85f;

  public String getRemoveBgApiKey() {
    return removeBgApiKey;
  }

  public void setRemoveBgApiKey(String removeBgApiKey) {
    this.removeBgApiKey = removeBgApiKey != null ? removeBgApiKey : "";
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

  public int getMaxResizeOutputSide() {
    return maxResizeOutputSide;
  }

  public void setMaxResizeOutputSide(int maxResizeOutputSide) {
    this.maxResizeOutputSide = maxResizeOutputSide;
  }

  public float getDefaultJpegOutputQuality() {
    return defaultJpegOutputQuality;
  }

  public void setDefaultJpegOutputQuality(float defaultJpegOutputQuality) {
    this.defaultJpegOutputQuality = defaultJpegOutputQuality;
  }
}
