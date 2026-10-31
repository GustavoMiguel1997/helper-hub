package io.helperhub.api.image.service;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import io.helperhub.api.image.dto.*;
import io.helperhub.api.image.exception.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageProcessingTest {
  @Test void decodesRealWebpAndResizesProportionally() throws Exception {
    var request = new ImageResizeRequest();
    try (var input = getClass().getResourceAsStream("/media/sample.webp")) {
      request.setFile(new MockMultipartFile("file", "sample.webp", "image/webp", input));
    }
    request.setMaxWidth(20);
    var result = new ImageResizeService(new ImageUsageProperties()).resize(request);
    try {
      var image = ImageIO.read(result.tempOutputFile().toFile());
      assertEquals(20, image.getWidth());
      assertEquals(10, image.getHeight());
      assertEquals("image/png", result.contentType());
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }

  @Test void convertsRealPngToJpegWithoutChangingDimensions() throws Exception {
    var request = new ImageConvertRequest();
    request.setFile(png());
    request.setOutputFormat(ImageResizeOutputFormat.JPEG);
    var result = new ImageResizeService(new ImageUsageProperties()).convert(request);
    try {
      byte[] bytes = Files.readAllBytes(result.tempOutputFile());
      assertEquals(0xff, bytes[0] & 255);
      assertEquals(0xd8, bytes[1] & 255);
      var image = ImageIO.read(result.tempOutputFile().toFile());
      assertEquals(40, image.getWidth());
      assertEquals(20, image.getHeight());
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }

  @Test void rejectsPixelBudgetBeforeDecoding() throws Exception {
    var props = new ImageUsageProperties(); props.setMaxInputPixels(100);
    var request = new ImageResizeRequest(); request.setFile(png()); request.setMaxWidth(10);
    assertEquals("IMAGE_PIXEL_LIMIT", assertThrows(ImageProcessingException.class,
        () -> new ImageResizeService(props).resize(request)).getError());
  }

  private static MockMultipartFile png() throws Exception {
    var buffer = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", buffer);
    return new MockMultipartFile("file", "sample.png", "image/png", buffer.toByteArray());
  }
}
