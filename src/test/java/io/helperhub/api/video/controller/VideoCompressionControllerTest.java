package io.helperhub.api.video.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.helperhub.api.video.dto.VideoCompressRequest;
import io.helperhub.api.video.dto.VideoFileOutputDto;
import io.helperhub.api.video.exception.VideoProcessingException;
import io.helperhub.api.video.service.VideoCompressionService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VideoCompressionController.class)
class VideoCompressionControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean VideoCompressionService service;

  @Test void rejectsMissingEmptyWrongExtensionMimeAndOversizedFiles() throws Exception {
    mvc.perform(multipart("/api/video/compress")).andExpect(status().isBadRequest());
    for (var file : new MockMultipartFile[] {
        new MockMultipartFile("file","empty.mp4","video/mp4",new byte[0]),
        new MockMultipartFile("file","script.mp4.exe","video/mp4",new byte[]{1}),
        new MockMultipartFile("file","movie.mp4","text/html",new byte[]{1}),
        new MockMultipartFile("file","movie.mp4","video/mp4",new byte[]{1}) {
          @Override public long getSize() { return VideoCompressRequest.MAX_UPLOAD_BYTES + 1; }
        }
    }) mvc.perform(multipart("/api/video/compress").file(file)).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test void rejectsInvalidResolutionQualityAndCommandArguments() throws Exception {
    for (var parameter : new String[][] {{"resolution","2160"},{"quality","--help"},{"resolution","720;exit"}}) {
      mvc.perform(multipart("/api/video/compress").file(validUpload()).param(parameter[0],parameter[1]))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(service);
  }

  @Test void sendsAttachmentAndDeletesDeliveredFile(@TempDir Path temp) throws Exception {
    Path output = Files.write(temp.resolve("out.mp4"),new byte[]{1,2,3});
    when(service.compress(any())).thenReturn(new VideoFileOutputDto(output,"video/mp4","movie-compressed.mp4"));
    mvc.perform(multipart("/api/video/compress").file(validUpload()).param("resolution","480").param("quality","SMALL"))
        .andExpect(status().isOk()).andExpect(content().contentType("video/mp4"))
        .andExpect(header().string("Content-Length","3"))
        .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")));
    assertFalse(Files.exists(output));
    verify(service).compress(argThat(request -> request.getResolution() == 480 && request.getQuality() == VideoCompressRequest.Quality.SMALL));
  }

  @Test void reportsCorruptVideoAndUnavailableTools() throws Exception {
    when(service.compress(any())).thenThrow(new VideoProcessingException("VIDEO_PROCESSING_FAILED","Vídeo inválido."));
    mvc.perform(multipart("/api/video/compress").file(validUpload()))
        .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("VIDEO_PROCESSING_FAILED"));
    doThrow(new VideoProcessingException("VIDEO_TOOLS_UNAVAILABLE","Indisponível.")).when(service).compress(any());
    mvc.perform(multipart("/api/video/compress").file(validUpload())).andExpect(status().isServiceUnavailable());
  }

  private static MockMultipartFile validUpload() {
    return new MockMultipartFile("file","movie.mp4","video/mp4",new byte[]{1});
  }
}
