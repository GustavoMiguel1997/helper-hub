package io.helperhub.api.audio.dto;

import io.helperhub.api.audio.domain.AudioOutputFormat;
import io.helperhub.api.audio.validation.AudioUploadSpecifications;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AudioMixRequest {
  @NotNull
  @Size(min = 1, max = 5, message = "Envie de 1 a 5 arquivos.")
  private List<@NotNull MultipartFile> files;
  @NotNull
  private AudioOutputFormat targetFormat = AudioOutputFormat.MP3;
  @NotNull @Min(0) @Max(200)
  private Integer volumePercent = 100;
  @NotNull @Min(0) @Max(30000)
  private Integer fadeInMs = 0;
  @NotNull @Min(0) @Max(30000)
  private Integer fadeOutMs = 0;

  @AssertTrue(message = "Selecione arquivos de áudio ou vídeo com áudio, não vazios e de formato aceito.")
  public boolean isUploadValid() {
    return files == null || files.stream().allMatch(file -> file != null && !file.isEmpty()
        && AudioUploadSpecifications.validateUploadedFile(file).isEmpty());
  }

  @AssertTrue(message = "Os arquivos devem somar até 50 MB.")
  public boolean isTotalSizeValid() {
    return files == null || files.stream().filter(java.util.Objects::nonNull)
        .mapToLong(MultipartFile::getSize).sum() <= AudioUploadSpecifications.MAX_UPLOAD_BYTES;
  }
}
