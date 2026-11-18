package io.helperhub.api.image.dto;

import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ImageBatchRequest {
  public enum Operation { COMPRESS, CONVERT, RESIZE }

  @NotNull(message = "Selecione as imagens do lote.")
  @Size(min = 1, max = 5, message = "Selecione de 1 a 5 imagens.")
  private List<@NotNull MultipartFile> files;

  @NotNull(message = "Selecione a operação do lote.")
  private Operation operation;
  private ImageResizeOutputFormat outputFormat;
  private Integer jpegQuality;
  private Integer targetBytes;
  private boolean allowResize;
  private Integer maxWidth;
  private Integer maxHeight;
}
