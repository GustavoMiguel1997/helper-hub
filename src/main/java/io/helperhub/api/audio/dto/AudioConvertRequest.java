package io.helperhub.api.audio.dto;

import io.helperhub.api.audio.domain.AudioOutputFormat;
import io.helperhub.api.audio.validation.ValidAudioConvertRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ValidAudioConvertRequest
public class AudioConvertRequest {

  @NotNull(message = "O arquivo é obrigatório.")
  private MultipartFile file;

  @NotNull(message = "targetFormat é obrigatório.")
  @Schema(
      description = "Formato desejado do arquivo de saída",
      allowableValues = {"MP3", "WAV", "AAC", "OGG", "FLAC", "OPUS"}
  )
  private AudioOutputFormat targetFormat;
}
