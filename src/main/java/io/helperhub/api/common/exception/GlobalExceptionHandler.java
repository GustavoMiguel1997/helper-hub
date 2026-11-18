package io.helperhub.api.common.exception;

import io.helperhub.api.audio.exception.AudioProcessingException;
import io.helperhub.api.image.exception.ImageProcessingException;
import io.helperhub.api.video.exception.VideoProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final int HTTP_STATUS_PAYLOAD_TOO_LARGE = 413;
  private static final int HTTP_STATUS_UNPROCESSABLE_ENTITY = 422;
  private static final int HTTP_STATUS_SERVICE_UNAVAILABLE = 503;

  private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  private static final String INVALID_TARGET_FORMAT = "INVALID_TARGET_FORMAT";
  private static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
  private static final String MISSING_PART = "MISSING_PART";
  private static final String IO_ERROR = "IO_ERROR";

  @ExceptionHandler(UncheckedIOException.class)
  public ResponseEntity<ApiError> handleUncheckedIo(UncheckedIOException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(IO_ERROR, "Erro ao preparar o arquivo de resposta."));
  }

  @ExceptionHandler(ConversionFailedException.class)
  public ResponseEntity<ApiError> handleConversionFailed(ConversionFailedException exception) {
    Throwable cause = exception.getCause();
    String message = Objects.nonNull(cause) && Objects.nonNull(cause.getMessage())
        ? cause.getMessage()
        : "Valor inválido.";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of(INVALID_TARGET_FORMAT, message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
    List<String> details = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.toList());
    ApiError body = ApiError.of(VALIDATION_FAILED, "Os dados enviados são inválidos.", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException exception) {
    String partName = exception.getRequestPartName();
    String message = Objects.nonNull(partName)
        ? "Parte obrigatória ausente: " + partName
        : "Parte obrigatória ausente.";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(MISSING_PART, message));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
    return ResponseEntity.status(HTTP_STATUS_PAYLOAD_TOO_LARGE)
        .body(ApiError.of(PAYLOAD_TOO_LARGE, "O arquivo enviado excede o tamanho máximo permitido."));
  }

  @ExceptionHandler(AudioProcessingException.class)
  public ResponseEntity<ApiError> handleAudioProcessing(AudioProcessingException exception) {
    return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY)
        .body(ApiError.of(exception.getError(), exception.getMessage()));
  }

  @ExceptionHandler(ImageProcessingException.class)
  public ResponseEntity<ApiError> handleImageProcessing(ImageProcessingException exception) {
    int status = "REMBG_NOT_CONFIGURED".equals(exception.getError())
        ? HTTP_STATUS_SERVICE_UNAVAILABLE
        : HTTP_STATUS_UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status)
        .body(ApiError.of(exception.getError(), exception.getMessage()));
  }

  @ExceptionHandler(VideoProcessingException.class)
  public ResponseEntity<?> handleVideoProcessing(VideoProcessingException exception, HttpServletRequest request) {
    int status = "VIDEO_TOOLS_UNAVAILABLE".equals(exception.getError())
        ? HTTP_STATUS_SERVICE_UNAVAILABLE : HTTP_STATUS_UNPROCESSABLE_ENTITY;
    if (!acceptsJson(request)) {
      return ResponseEntity.status(status).build();
    }
    return ResponseEntity.status(status)
        .body(ApiError.of(exception.getError(), exception.getMessage()));
  }

  private static boolean acceptsJson(HttpServletRequest request) {
    String acceptHeader = request.getHeader("Accept");
    if (Objects.isNull(acceptHeader) || acceptHeader.isBlank()) {
      return true;
    }
    try {
      List<MediaType> accepted = MediaType.parseMediaTypes(acceptHeader);
      for (MediaType mediaType : accepted) {
        if (mediaType.includes(MediaType.APPLICATION_JSON)
            || mediaType.includes(MediaType.ALL)
            || MediaType.APPLICATION_JSON.includes(mediaType)) {
          return true;
        }
      }
      return false;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
