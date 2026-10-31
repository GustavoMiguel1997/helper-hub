package io.helperhub.api.audio.config;

import io.helperhub.api.audio.domain.AudioOutputFormat;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class AudioWebConfiguration {

  @Bean
  public Converter<String, AudioOutputFormat> audioOutputFormatConverter() {
    return new Converter<>() {
      @Override
      public AudioOutputFormat convert(String source) {
        if (Objects.isNull(source)) {
          return null;
        }
        if (source.isBlank()) {
          return null;
        }
        try {
          return AudioOutputFormat.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
          throw new IllegalArgumentException(
              "targetFormat deve ser um dos valores: MP3, WAV, AAC, OGG, FLAC, OPUS.");
        }
      }
    };
  }
}
