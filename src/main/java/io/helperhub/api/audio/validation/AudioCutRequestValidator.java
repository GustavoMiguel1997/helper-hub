package io.helperhub.api.audio.validation;

import io.helperhub.api.audio.dto.AudioCutRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import java.util.Optional;

public class AudioCutRequestValidator implements ConstraintValidator<ValidAudioCutRequest, AudioCutRequest> {

  @Override
  public boolean isValid(AudioCutRequest request, ConstraintValidatorContext context) {
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
    if (Objects.nonNull(request.getStartMs()) && Objects.nonNull(request.getEndMs())
        && request.getEndMs() <= request.getStartMs()) {
      context.buildConstraintViolationWithTemplate("endMs deve ser maior que startMs (intervalo de corte inválido).")
          .addPropertyNode("endMs")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
