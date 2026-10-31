package io.helperhub.api.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
    String error,
    String message,
    Instant timestamp,
    List<String> details
) {

  public ApiError {
    details = Objects.isNull(details) ? List.of() : List.copyOf(details);
  }

  public static ApiError of(String error, String message) {
    return new ApiError(error, message, Instant.now(), Collections.emptyList());
  }

  public static ApiError of(String error, String message, List<String> details) {
    return new ApiError(error, message, Instant.now(), details);
  }
}
