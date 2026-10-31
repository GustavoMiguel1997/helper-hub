package io.helperhub.api.video.dto;

import io.helperhub.api.video.domain.VideoOutputFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoYoutubeDownloadRequest {

  @NotBlank(message = "A URL é obrigatória.")
  private String url;

  private VideoOutputFormat outputFormat = VideoOutputFormat.MP4;
}
