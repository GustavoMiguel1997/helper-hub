package io.helperhub.api.image.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.helperhub.api.image.dto.ImageFileOutputDto;
import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.config.ImageWebConfiguration;
import io.helperhub.api.image.service.ImageRemoveBackgroundService;
import io.helperhub.api.image.service.ImageResizeService;
import io.helperhub.api.image.validation.ImageConvertRequestValidator;
import io.helperhub.api.image.validation.ImageResizeRequestValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ImageController.class)
@Import({ImageWebConfiguration.class, ImageResizeRequestValidator.class, ImageConvertRequestValidator.class})
@EnableConfigurationProperties(ImageUsageProperties.class)
@TestPropertySource(properties = "helperhub.image.rate-limit-enabled=false")
class ImageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ImageRemoveBackgroundService imageRemoveBackgroundService;

  @MockitoBean
  private ImageResizeService imageResizeService;

  @Test
  void returnsBadRequestWhenFileExtensionIsNotSupported() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "notes.txt",
        "text/plain",
        new byte[] {0x00, 0x01}
    );

    mockMvc.perform(multipart("/api/image/remove-background").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsBadRequestWhenFileIsMissing() throws Exception {
    mockMvc.perform(multipart("/api/image/remove-background"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnsOkWhenRequestIsValid(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("photo-nobg.png");
    Files.write(out, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
    when(imageRemoveBackgroundService.removeBackground(any())).thenReturn(
        new ImageFileOutputDto(out, MediaType.IMAGE_PNG_VALUE, "photo-nobg.png"));

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "photo.jpg",
        "image/jpeg",
        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}
    );

    mockMvc.perform(multipart("/api/image/remove-background").file(file))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
  }

  @Test
  void resizeReturnsBadRequestWhenNoDimensions() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "photo.jpg",
        "image/jpeg",
        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}
    );

    mockMvc.perform(multipart("/api/image/resize").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void convertReturnsOkWhenOutputFormatIsSet(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("photo-converted.png");
    Files.write(out, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
    when(imageResizeService.convert(any())).thenReturn(
        new ImageFileOutputDto(out, MediaType.IMAGE_PNG_VALUE, "photo-converted.png"));

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "photo.jpg",
        "image/jpeg",
        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}
    );

    mockMvc.perform(multipart("/api/image/convert")
            .file(file)
            .param("outputFormat", "PNG"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
  }

  @Test
  void convertReturnsBadRequestWhenOutputFormatMissing() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "photo.jpg",
        "image/jpeg",
        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}
    );

    mockMvc.perform(multipart("/api/image/convert").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
  }

  @Test
  void resizeReturnsOkWhenMaxWidthSet(@TempDir Path tempDir) throws Exception {
    Path out = tempDir.resolve("photo-resized.jpg");
    Files.write(out, new byte[] {1, 2, 3});
    when(imageResizeService.resize(any())).thenReturn(
        new ImageFileOutputDto(out, MediaType.IMAGE_JPEG_VALUE, "photo-resized.jpg"));

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "photo.jpg",
        "image/jpeg",
        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}
    );

    mockMvc.perform(multipart("/api/image/resize")
            .file(file)
            .param("maxWidth", "800"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG));
  }

  @Test void compressionDefaultsToJpegAndValidatesInheritedConstraints(@TempDir Path tempDir) throws Exception {
    var output = Files.write(tempDir.resolve("compressed.jpg"), new byte[]{1, 2, 3});
    when(imageResizeService.compress(any())).thenReturn(new ImageFileOutputDto(output, "image/jpeg", "compressed.jpg"));
    var file = new MockMultipartFile("file", "source.png", "image/png", new byte[]{1});
    mockMvc.perform(multipart("/api/image/compress").file(file)).andExpect(status().isOk());
    var argument = org.mockito.ArgumentCaptor.forClass(io.helperhub.api.image.dto.ImageCompressRequest.class);
    org.mockito.Mockito.verify(imageResizeService).compress(argument.capture());
    org.junit.jupiter.api.Assertions.assertEquals(75, argument.getValue().getJpegQuality());
    org.junit.jupiter.api.Assertions.assertEquals(io.helperhub.api.image.domain.ImageResizeOutputFormat.JPEG, argument.getValue().getOutputFormat());
    mockMvc.perform(multipart("/api/image/compress").file(file).param("jpegQuality", "0"))
        .andExpect(status().isBadRequest());
    mockMvc.perform(multipart("/api/image/compress")).andExpect(status().isBadRequest());
  }
}
