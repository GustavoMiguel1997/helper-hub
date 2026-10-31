package io.helperhub.api.audio.validation;

import io.helperhub.api.audio.dto.AudioConvertRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import java.util.Optional;

public class AudioConvertRequestValidator implements ConstraintValidator<ValidAudioConvertRequest, AudioConvertRequest> {

  @Override
  public boolean isValid(AudioConvertRequest request, ConstraintValidatorContext context) {
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
    Optional<String> uploadRule = AudioUploadSpecifications.validateUploadedFile(request.getFile());
    if (uploadRule.isPresent()) {
      context.buildConstraintViolationWithTemplate(uploadRule.get())
          .addPropertyNode("file")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
