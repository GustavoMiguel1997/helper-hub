package io.helperhub.api.audio.dto;

import io.helperhub.api.audio.validation.ValidAudioCutRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ValidAudioCutRequest
public class AudioCutRequest {

  @NotNull(message = "O arquivo é obrigatório.")
  private MultipartFile file;

  @NotNull(message = "startMs é obrigatório.")
  @Min(value = 0, message = "startMs deve ser maior ou igual a zero.")
  private Long startMs;

  @NotNull(message = "endMs é obrigatório.")
  @Min(value = 0, message = "endMs deve ser maior ou igual a zero.")
  private Long endMs;
}
