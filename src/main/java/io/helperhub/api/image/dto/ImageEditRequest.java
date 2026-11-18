package io.helperhub.api.image.dto;

import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageEditRequest extends ImageConvertRequest {
  @Min(0)
  private Integer x;
  @Min(0)
  private Integer y;
  @Min(1)
  private Integer width;
  @Min(1)
  private Integer height;
  private int rotation;
  private boolean flipHorizontal;
  private boolean flipVertical;

  public ImageEditRequest() {
    setOutputFormat(ImageResizeOutputFormat.PNG);
  }

  @AssertTrue(message = "Informe x, y, width e height juntos para recortar.")
  public boolean isCropComplete() {
    return (x == null && y == null && width == null && height == null)
        || (x != null && y != null && width != null && height != null);
  }

  @AssertTrue(message = "rotation deve ser 0, 90, 180 ou 270.")
  public boolean isRotationValid() {
    return rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270;
  }
}
