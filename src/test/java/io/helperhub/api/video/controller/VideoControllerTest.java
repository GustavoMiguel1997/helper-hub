package io.helperhub.api.video.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.helperhub.api.video.config.VideoUsageProperties;
import io.helperhub.api.video.config.VideoWebConfiguration;
import io.helperhub.api.video.dto.VideoFileOutputDto;
import io.helperhub.api.video.exception.VideoProcessingException;
import io.helperhub.api.video.service.VideoService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VideoController.class)
@Import(VideoWebConfiguration.class)
@EnableConfigurationProperties(VideoUsageProperties.class)
@TestPropertySource(properties = "helperhub.video.rate-limit-enabled=false")
class VideoControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private VideoService videoService;

  @Test
  void returnsBadRequestWhenUrlMissing() throws Exception {
    mockMvc.perform(post("/api/video/youtube-download")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsOkWhenRequestIsValid(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("youtube-video.mp4");
    Files.write(out, new byte[] {1, 2, 3});
    when(videoService.downloadFromYoutube(any()))
        .thenReturn(new VideoFileOutputDto(out, "video/mp4", "youtube-video.mp4"));

    mockMvc.perform(post("/api/video/youtube-download")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("url", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .param("outputFormat", "MP4"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("video/mp4"));
  }

  @Test
  void returnsUnprocessableEntityWhenClientAcceptsOnlyVideoMediaType() throws Exception {
    when(videoService.downloadFromYoutube(any()))
        .thenThrow(new VideoProcessingException(
            "YTDLP_NOT_AVAILABLE",
            "yt-dlp não está instalado ou não está acessível."));

    mockMvc.perform(post("/api/video/youtube-download")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept("video/mp4")
            .param("url", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .param("outputFormat", "MP4"))
        .andExpect(status().is(422))
        .andExpect(content().string(""));
  }
}
