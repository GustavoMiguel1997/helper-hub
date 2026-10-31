package io.helperhub.api.image.exception;

public class ImageProcessingException extends RuntimeException {

  private final String error;

  public ImageProcessingException(String error, String message) {
    super(message);
    this.error = error;
  }

  public ImageProcessingException(String error, String message, Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  public String getError() {
    return error;
  }
}
