package io.helperhub.api.video.exception;

public class VideoProcessingException extends RuntimeException {

  private final String error;

  public VideoProcessingException(String error, String message) {
    super(message);
    this.error = error;
  }

  public VideoProcessingException(String error, String message, Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  public String getError() {
    return error;
  }
}
