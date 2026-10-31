package io.helperhub.api.audio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.helperhub.api.audio.config.AudioWebConfiguration;
import io.helperhub.api.audio.dto.AudioFileOutputDto;
import io.helperhub.api.audio.service.AudioService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AudioController.class)
@Import(AudioWebConfiguration.class)
@TestPropertySource(properties = "helperhub.audio.rate-limit-enabled=false")
class AudioControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AudioService audioService;

  @Test
  void returnsBadRequestWhenIntervalIsInvalid() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "clip.mp3",
        MediaType.APPLICATION_OCTET_STREAM_VALUE,
        new byte[] {0x00, 0x01}
    );

    mockMvc.perform(multipart("/api/audio/cut")
            .file(file)
            .param("startMs", "500")
            .param("endMs", "100"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsBadRequestWhenFileIsMissing() throws Exception {
    mockMvc.perform(multipart("/api/audio/cut")
            .param("startMs", "0")
            .param("endMs", "1000"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnsBadRequestWhenFileExtensionIsNotSupported() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "notes.txt",
        "text/plain",
        new byte[] {0x00, 0x01}
    );

    mockMvc.perform(multipart("/api/audio/cut")
            .file(file)
            .param("startMs", "0")
            .param("endMs", "1000"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsOkWhenRequestIsValid(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("clip-cut.mp3");
    Files.write(out, new byte[] {9, 9, 9});
    when(audioService.cutAudio(any())).thenReturn(
        new AudioFileOutputDto(out, MediaType.APPLICATION_OCTET_STREAM_VALUE, "clip-cut.mp3"));

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "clip.mp3",
        "audio/mpeg",
        new byte[] {0x00, 0x01}
    );

    mockMvc.perform(multipart("/api/audio/cut")
            .file(file)
            .param("startMs", "0")
            .param("endMs", "1000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
  }

  @Test
  void convertReturnsBadRequestWhenFileExtensionIsNotSupported() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "notes.txt",
        "text/plain",
        new byte[] {0x00, 0x01}
    );

    mockMvc.perform(multipart("/api/audio/convert")
            .file(file)
            .param("targetFormat", "MP3"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void convertReturnsBadRequestWhenFileIsEmpty() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "clip.mp3",
        "audio/mpeg",
        new byte[0]
    );

    mockMvc.perform(multipart("/api/audio/convert")
            .file(file)
            .param("targetFormat", "WAV"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void convertReturnsOkWhenRequestIsValid(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("clip-converted.wav");
    Files.write(out, new byte[] {1, 2, 3});
    when(audioService.convertAudio(any())).thenReturn(
        new AudioFileOutputDto(out, "audio/wav", "clip-converted.wav"));

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "clip.mp3",
        "audio/mpeg",
        new byte[] {0x00, 0x01}
    );

    mockMvc.perform(multipart("/api/audio/convert")
            .file(file)
            .param("targetFormat", "WAV"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("audio/wav"));
  }
}
