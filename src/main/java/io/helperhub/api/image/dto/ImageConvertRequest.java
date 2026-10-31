package io.helperhub.api.image.dto;

import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import io.helperhub.api.image.validation.ValidImageConvertRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ValidImageConvertRequest
public class ImageConvertRequest {

  @NotNull(message = "O arquivo é obrigatório.")
  private MultipartFile file;

  private ImageResizeOutputFormat outputFormat;

  /**
   * Qualidade JPEG 1–100 (só aplica quando a saída for JPEG).
   */
  private Integer jpegQuality;
}
