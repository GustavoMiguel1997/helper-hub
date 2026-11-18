package io.helperhub.api.image.validation;

import io.helperhub.api.image.dto.ImageConvertRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import java.util.Optional;

public class ImageConvertRequestValidator
    implements ConstraintValidator<ValidImageConvertRequest, ImageConvertRequest> {

  @Override
  public boolean isValid(ImageConvertRequest request, ConstraintValidatorContext context) {
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
    if (Objects.isNull(request.getOutputFormat()) || request.getOutputFormat().isAuto()) {
      context.buildConstraintViolationWithTemplate("outputFormat deve ser JPEG, PNG ou WEBP.")
          .addPropertyNode("outputFormat")
          .addConstraintViolation();
      return false;
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
