package io.helperhub.api.image.service;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.dto.ImageCompressRequest;
import io.helperhub.api.image.exception.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageCompressionTest {
  @Test void qualityControlsSizeWithoutResizing() throws Exception {
    var source = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
    var random = new Random(42);
    for (int y = 0; y < source.getHeight(); y++)
      for (int x = 0; x < source.getWidth(); x++) source.setRGB(x, y, random.nextInt());
    var buffer = new ByteArrayOutputStream(); ImageIO.write(source, "png", buffer);
    var request = new ImageCompressRequest();
    request.setFile(new MockMultipartFile("file", "noise.png", "image/png", buffer.toByteArray()));
    var service = new ImageResizeService(new ImageUsageProperties());
    request.setJpegQuality(95); var high = service.compress(request);
    try {
      request.setJpegQuality(40); var low = service.compress(request);
      try {
        assertTrue(Files.size(low.tempOutputFile()) < Files.size(high.tempOutputFile()));
        assertTrue(Files.size(low.tempOutputFile()) < buffer.size());
        var image = ImageIO.read(low.tempOutputFile().toFile());
        assertEquals(320, image.getWidth()); assertEquals(180, image.getHeight());
        assertEquals("noise-compressed.jpg", low.downloadFilename());
        assertEquals("image/jpeg", low.contentType());
      } finally { Files.deleteIfExists(low.tempOutputFile()); }
    } finally { Files.deleteIfExists(high.tempOutputFile()); }
  }

  @Test void transparentPixelsBecomeWhiteInJpeg() throws Exception {
    var source = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
    var buffer = new ByteArrayOutputStream(); ImageIO.write(source, "png", buffer);
    var request = new ImageCompressRequest();
    request.setFile(new MockMultipartFile("file", "transparent.png", "image/png", buffer.toByteArray()));
    var result = new ImageResizeService(new ImageUsageProperties()).compress(request);
    try {
      var image = ImageIO.read(result.tempOutputFile().toFile());
      assertEquals(0xffffff, image.getRGB(10, 10) & 0xffffff);
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }

  @Test void targetIsEnforcedAndResizingRequiresOptIn() throws Exception {
    var source = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
    var random = new Random(42);
    for (int y = 0; y < source.getHeight(); y++) {
      for (int x = 0; x < source.getWidth(); x++) {
        source.setRGB(x, y, random.nextInt());
      }
    }
    var buffer = new ByteArrayOutputStream();
    ImageIO.write(source, "png", buffer);
    var request = new ImageCompressRequest();
    request.setFile(new MockMultipartFile("file", "noise.png", "image/png", buffer.toByteArray()));
    var service = new ImageResizeService(new ImageUsageProperties());

    request.setTargetBytes(10_000);
    var fullSize = service.compress(request);
    try {
      assertTrue(Files.size(fullSize.tempOutputFile()) <= 10_000);
      var image = ImageIO.read(fullSize.tempOutputFile().toFile());
      assertEquals(320, image.getWidth());
      assertEquals(180, image.getHeight());
    } finally {
      Files.deleteIfExists(fullSize.tempOutputFile());
    }

    request.setTargetBytes(1024);
    var failure = assertThrows(ImageProcessingException.class, () -> service.compress(request));
    assertEquals("IMAGE_TARGET_UNREACHABLE", failure.getError());

    request.setAllowResize(true);
    var smaller = service.compress(request);
    try {
      assertTrue(Files.size(smaller.tempOutputFile()) <= 1024);
      var image = ImageIO.read(smaller.tempOutputFile().toFile());
      assertTrue(image.getWidth() < 320);
      assertTrue(image.getHeight() < 180);
    } finally {
      Files.deleteIfExists(smaller.tempOutputFile());
    }
  }
}
