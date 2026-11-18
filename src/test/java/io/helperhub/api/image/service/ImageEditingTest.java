package io.helperhub.api.image.service;

import static org.junit.jupiter.api.Assertions.*;

import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import io.helperhub.api.image.dto.ImageEditRequest;
import io.helperhub.api.image.exception.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageEditingTest {
  private final ImageResizeService service = new ImageResizeService(new ImageUsageProperties());

  private ImageEditRequest request() throws Exception {
    var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, 0xffff0000);
    image.setRGB(1, 0, 0xff00ff00);
    image.setRGB(2, 0, 0xff0000ff);
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    var request = new ImageEditRequest();
    request.setFile(new MockMultipartFile("file", "source.png", "image/png", bytes.toByteArray()));
    return request;
  }

  @Test
  void cropAndClockwiseRotationMatchSelectedPixels() throws Exception {
    var request = request();
    request.setX(1);
    request.setY(0);
    request.setWidth(2);
    request.setHeight(1);
    request.setRotation(90);
    var result = service.edit(request);
    try {
      var image = ImageIO.read(result.tempOutputFile().toFile());
      assertEquals(1, image.getWidth());
      assertEquals(2, image.getHeight());
      assertEquals(0xff00ff00, image.getRGB(0, 0));
      assertEquals(0xff0000ff, image.getRGB(0, 1));
    } finally {
      Files.deleteIfExists(result.tempOutputFile());
    }
  }

  @Test
  void flipPreservesAlphaAndRejectsOutOfBoundsCrop() throws Exception {
    var request = request();
    request.setFlipHorizontal(true);
    var result = service.edit(request);
    try {
      var image = ImageIO.read(result.tempOutputFile().toFile());
      assertEquals(0xff0000ff, image.getRGB(0, 0));
      assertEquals(0, image.getRGB(0, 1));
    } finally {
      Files.deleteIfExists(result.tempOutputFile());
    }
    request.setX(Integer.MAX_VALUE);
    request.setY(0);
    request.setWidth(2);
    request.setHeight(1);
    assertEquals("IMAGE_CROP_BOUNDS",
        assertThrows(ImageProcessingException.class, () -> service.edit(request)).getError());
  }

  @Test
  void webpIsDecodableAndEncodesAlpha() throws Exception {
    var request = request();
    request.setOutputFormat(ImageResizeOutputFormat.WEBP);
    var result = service.edit(request);
    try {
      assertEquals("image/webp", result.contentType());
      assertTrue(result.downloadFilename().endsWith(".webp"));
      var image = ImageIO.read(result.tempOutputFile().toFile());
      assertEquals(3, image.getWidth());
      assertEquals(2, image.getHeight());
      var bytes = Files.readAllBytes(result.tempOutputFile());
      assertTrue(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("ALPH"));
      var roundTrip = new ImageEditRequest();
      roundTrip.setFile(new MockMultipartFile("file", "result.webp", "image/webp", bytes));
      var png = service.edit(roundTrip);
      try {
        var decoded = ImageIO.read(png.tempOutputFile().toFile());
        assertEquals(0, decoded.getRGB(0, 1) >>> 24);
      } finally {
        Files.deleteIfExists(png.tempOutputFile());
      }
    } finally {
      Files.deleteIfExists(result.tempOutputFile());
    }
  }
}
