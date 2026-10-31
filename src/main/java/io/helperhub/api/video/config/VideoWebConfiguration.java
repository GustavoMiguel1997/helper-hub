package io.helperhub.api.video.config;

import io.helperhub.api.video.domain.VideoOutputFormat;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class VideoWebConfiguration {

  @Bean
  public Converter<String, VideoOutputFormat> videoOutputFormatConverter() {
    return new Converter<>() {
      @Override
      public VideoOutputFormat convert(String source) {
        if (Objects.isNull(source) || source.isBlank()) {
          return VideoOutputFormat.MP4;
        }
        try {
          return VideoOutputFormat.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
          throw new IllegalArgumentException("outputFormat deve ser um dos valores: MP4, WEBM.");
        }
      }
    };
  }
}
