package io.helperhub.api.image.dto;

import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** Compression defaults; inherited upload, format and quality validation still applies. */
@Getter
@Setter
public class ImageCompressRequest extends ImageConvertRequest {
  @Min(value = 1024, message = "O tamanho-alvo mínimo é 1024 bytes.")
  @Max(value = 10_000_000, message = "O tamanho-alvo máximo é 10 MB.")
  private Integer targetBytes;
  private boolean allowResize;

  public ImageCompressRequest() {
    setOutputFormat(ImageResizeOutputFormat.JPEG);
    setJpegQuality(75);
  }

  @AssertTrue(message = "A compressão por tamanho-alvo requer saída JPEG.")
  public boolean isTargetFormatValid() {
    return targetBytes == null || getOutputFormat() == ImageResizeOutputFormat.JPEG;
  }
}
