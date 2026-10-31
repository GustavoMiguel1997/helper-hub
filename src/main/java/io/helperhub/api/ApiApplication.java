package io.helperhub.api;

import io.helperhub.api.audio.config.AudioUsageProperties;
import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.video.config.VideoUsageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AudioUsageProperties.class, ImageUsageProperties.class, VideoUsageProperties.class})
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
