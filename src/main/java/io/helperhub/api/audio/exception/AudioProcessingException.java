package io.helperhub.api.audio.exception;

public class AudioProcessingException extends RuntimeException {

  private final String error;

  public AudioProcessingException(String error, String message) {
    super(message);
    this.error = error;
  }

  public AudioProcessingException(String error, String message, Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  public String getError() {
    return error;
  }
}
