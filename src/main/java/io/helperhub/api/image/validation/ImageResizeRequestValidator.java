package io.helperhub.api.image.validation;

import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.dto.ImageResizeRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ImageResizeRequestValidator implements ConstraintValidator<ValidImageResizeRequest, ImageResizeRequest> {

  private final ImageUsageProperties imageUsageProperties;

  public ImageResizeRequestValidator(ImageUsageProperties imageUsageProperties) {
    this.imageUsageProperties = imageUsageProperties;
  }

  @Override
  public boolean isValid(ImageResizeRequest request, ConstraintValidatorContext context) {
    if (Objects.isNull(request)) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    if (Objects.isNull(request.getFile()) || request.getFile().isEmpty()) {
      context.buildConstraintViolationWithTemplate("O arquivo não pode estar vazio.")
          .addPropertyNode("file")
          .addConstraintViolation();
      return false;
    }
    Optional<String> uploadRule = ImageUploadSpecifications.validateUploadedFile(request.getFile());
    if (uploadRule.isPresent()) {
      context.buildConstraintViolationWithTemplate(uploadRule.get())
          .addPropertyNode("file")
          .addConstraintViolation();
      return false;
    }
    Integer maxSide = imageUsageProperties.getMaxResizeOutputSide();
    if (Objects.isNull(request.getMaxWidth()) && Objects.isNull(request.getMaxHeight())) {
      context.buildConstraintViolationWithTemplate("Informe maxWidth e/ou maxHeight (pelo menos um).")
          .addPropertyNode("maxWidth")
          .addConstraintViolation();
      return false;
    }
    if (Objects.nonNull(request.getMaxWidth())) {
      if (request.getMaxWidth() < 1 || request.getMaxWidth() > maxSide) {
        context.buildConstraintViolationWithTemplate(
                "maxWidth deve estar entre 1 e " + maxSide + ".")
            .addPropertyNode("maxWidth")
            .addConstraintViolation();
        return false;
      }
    }
    if (Objects.nonNull(request.getMaxHeight())) {
      if (request.getMaxHeight() < 1 || request.getMaxHeight() > maxSide) {
        context.buildConstraintViolationWithTemplate(
                "maxHeight deve estar entre 1 e " + maxSide + ".")
            .addPropertyNode("maxHeight")
            .addConstraintViolation();
        return false;
      }
    }
    if (Objects.nonNull(request.getJpegQuality())) {
      int q = request.getJpegQuality();
      if (q < 1 || q > 100) {
        context.buildConstraintViolationWithTemplate("jpegQuality deve estar entre 1 e 100.")
            .addPropertyNode("jpegQuality")
            .addConstraintViolation();
        return false;
      }
    }
    return true;
  }
}
