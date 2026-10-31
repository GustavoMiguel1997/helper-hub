package io.helperhub.api.image.dto;

import io.helperhub.api.image.validation.ValidImageRemoveBackgroundRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ValidImageRemoveBackgroundRequest
public class ImageRemoveBackgroundRequest {

  @NotNull(message = "O arquivo é obrigatório.")
  private MultipartFile file;
}
