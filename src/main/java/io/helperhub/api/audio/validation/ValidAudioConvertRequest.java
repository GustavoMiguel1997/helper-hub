package io.helperhub.api.audio.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AudioConvertRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAudioConvertRequest {

  String message() default "Invalid audio convert request";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
