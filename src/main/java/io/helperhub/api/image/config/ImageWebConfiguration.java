package io.helperhub.api.image.config;

import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class ImageWebConfiguration {

  @Bean
  public Converter<String, ImageResizeOutputFormat> imageResizeOutputFormatConverter() {
    return new Converter<>() {
      @Override
      public ImageResizeOutputFormat convert(String source) {
        if (Objects.isNull(source) || source.isBlank()) {
          return ImageResizeOutputFormat.AUTO;
        }
        try {
          return ImageResizeOutputFormat.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
          throw new IllegalArgumentException(
              "outputFormat deve ser um dos valores: AUTO, JPEG, PNG.");
        }
      }
    };
  }
}
