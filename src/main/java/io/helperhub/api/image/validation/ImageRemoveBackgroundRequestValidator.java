package io.helperhub.api.image.validation;

import io.helperhub.api.image.dto.ImageRemoveBackgroundRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import java.util.Optional;

public class ImageRemoveBackgroundRequestValidator
    implements ConstraintValidator<ValidImageRemoveBackgroundRequest, ImageRemoveBackgroundRequest> {

  @Override
  public boolean isValid(ImageRemoveBackgroundRequest request, ConstraintValidatorContext context) {
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
    return true;
  }
}
