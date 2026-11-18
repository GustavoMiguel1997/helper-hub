package io.helperhub.api.image.service;

import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.domain.ImageResizeOutputFormat;
import io.helperhub.api.image.dto.ImageCompressRequest;
import io.helperhub.api.image.dto.ImageConvertRequest;
import io.helperhub.api.image.dto.ImageFileOutputDto;
import io.helperhub.api.image.dto.ImageResizeRequest;
import io.helperhub.api.image.exception.ImageProcessingException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageResizeService {

  private static final Logger log = LoggerFactory.getLogger(ImageResizeService.class);

  private final ImageUsageProperties imageUsageProperties;

  public ImageResizeService(ImageUsageProperties imageUsageProperties) {
    this.imageUsageProperties = imageUsageProperties;
  }

  public ImageFileOutputDto resize(ImageResizeRequest request) {
    MultipartFile multipart = request.getFile();
    ImageResizeOutputFormat resolved = resolveOutputFormat(request.getOutputFormat(), multipart);
    Path inputTemp = null;
    Path outputTemp = null;
    boolean outputDelivered = false;
    try {
      String inSuffix = extensionSuffixFromOriginalName(multipart.getOriginalFilename());
      inputTemp = Files.createTempFile("helperhub-img-resize-in-", inSuffix);
      outputTemp = Files.createTempFile("helperhub-img-resize-out-", "." + resolved.getFileExtension());
      multipart.transferTo(inputTemp);

      BufferedImage source = readBoundedImage(inputTemp);
      if (Objects.isNull(source) || source.getWidth() <= 0 || source.getHeight() <= 0) {
        throw new ImageProcessingException(
            "IMAGE_READ_FAILED",
            "Não foi possível ler a imagem (formato inválido ou corrompido).");
      }

      int srcW = source.getWidth();
      int srcH = source.getHeight();
      double scale = computeDownscaleOnlyScale(srcW, srcH, request.getMaxWidth(), request.getMaxHeight());
      int targetW = Math.max(1, (int) Math.round(srcW * scale));
      int targetH = Math.max(1, (int) Math.round(srcH * scale));

      Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(resolved == ImageResizeOutputFormat.JPEG ? flattenTransparency(source) : source).size(targetW, targetH);
      if (resolved == ImageResizeOutputFormat.JPEG) {
        builder.outputQuality(resolveJpegQuality(request.getJpegQuality()));
      }
      builder.outputFormat(resolved.getThumbnailFormat()).toFile(outputTemp.toFile());

      String downloadName = buildDownloadFilename(
          multipart.getOriginalFilename(),
          resolved.getFileExtension(),
          "resized");
      ImageFileOutputDto result = new ImageFileOutputDto(outputTemp, resolved.getMediaType(), downloadName);
      outputDelivered = true;
      return result;
    } catch (ImageProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Failed to resize image", exception);
      throw new ImageProcessingException(
          "IMAGE_RESIZE_FAILED",
          "Não foi possível redimensionar a imagem. Verifique se o arquivo é uma imagem válida.",
          exception);
    } finally {
      deleteQuietly(inputTemp);
      if (!outputDelivered) {
        deleteQuietly(outputTemp);
      }
    }
  }

  public ImageFileOutputDto compress(ImageCompressRequest request) {
    if (request.getTargetBytes() != null) {
      return compressToTarget(request);
    }
    ImageFileOutputDto output = convert(request);
    return new ImageFileOutputDto(output.tempOutputFile(), output.contentType(),
        buildDownloadFilename(request.getFile().getOriginalFilename(), request.getOutputFormat().getFileExtension(), "compressed"));
  }

  private ImageFileOutputDto compressToTarget(ImageCompressRequest request) {
    MultipartFile multipart = request.getFile();
    Path input = null;
    Path trial = null;
    Path best = null;
    boolean outputDelivered = false;
    try {
      input = Files.createTempFile("helperhub-img-target-in-", ".bin");
      trial = Files.createTempFile("helperhub-img-target-trial-", ".jpg");
      best = Files.createTempFile("helperhub-img-target-out-", ".jpg");
      multipart.transferTo(input);
      BufferedImage source = flattenTransparency(readBoundedImage(input));
      BufferedImage candidate = source;
      int minimum = 1;
      double scale = 1;
      while (!encodeWithinTarget(candidate, minimum, request.getTargetBytes(), best)) {
        if (!request.isAllowResize() || (candidate.getWidth() == 1 && candidate.getHeight() == 1)) {
          throw new ImageProcessingException("IMAGE_TARGET_UNREACHABLE",
              "Não foi possível atingir o tamanho-alvo. Aumente o limite ou permita reduzir as dimensões.");
        }
        scale *= 0.75;
        int width = Math.max(1, (int) (source.getWidth() * scale));
        int height = Math.max(1, (int) (source.getHeight() * scale));
        candidate = Thumbnails.of(source).forceSize(width, height).asBufferedImage();
      }
      int low = minimum + 1;
      int high = 99;
      if (encodeWithinTarget(candidate, 100, request.getTargetBytes(), trial)) {
        Files.copy(trial, best, StandardCopyOption.REPLACE_EXISTING);
        low = 100;
      }
      while (low <= high) {
        int quality = (low + high) / 2;
        if (encodeWithinTarget(candidate, quality, request.getTargetBytes(), trial)) {
          Files.copy(trial, best, StandardCopyOption.REPLACE_EXISTING);
          low = quality + 1;
        } else {
          high = quality - 1;
        }
      }
      ImageFileOutputDto result = new ImageFileOutputDto(best, "image/jpeg",
          buildDownloadFilename(multipart.getOriginalFilename(), "jpg", "compressed"));
      outputDelivered = true;
      return result;
    } catch (IOException exception) {
      throw new ImageProcessingException("IMAGE_COMPRESS_FAILED", "Não foi possível comprimir a imagem.", exception);
    } finally {
      deleteQuietly(input);
      deleteQuietly(trial);
      if (!outputDelivered) {
        deleteQuietly(best);
      }
    }
  }

  private static boolean encodeWithinTarget(BufferedImage image, int quality, int targetBytes, Path output)
      throws IOException {
    Thumbnails.of(image).scale(1).outputQuality(quality / 100f).outputFormat("jpg").toFile(output.toFile());
    return Files.size(output) <= targetBytes;
  }

  public ImageFileOutputDto convert(ImageConvertRequest request) {
    MultipartFile multipart = request.getFile();
    ImageResizeOutputFormat resolved = request.getOutputFormat();
    Path inputTemp = null;
    Path outputTemp = null;
    boolean outputDelivered = false;
    try {
      String inSuffix = extensionSuffixFromOriginalName(multipart.getOriginalFilename());
      inputTemp = Files.createTempFile("helperhub-img-convert-in-", inSuffix);
      outputTemp = Files.createTempFile("helperhub-img-convert-out-", "." + resolved.getFileExtension());
      multipart.transferTo(inputTemp);

      BufferedImage source = readBoundedImage(inputTemp);
      if (Objects.isNull(source) || source.getWidth() <= 0 || source.getHeight() <= 0) {
        throw new ImageProcessingException(
            "IMAGE_READ_FAILED",
            "Não foi possível ler a imagem (formato inválido ou corrompido).");
      }

      Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(resolved == ImageResizeOutputFormat.JPEG ? flattenTransparency(source) : source).size(source.getWidth(), source.getHeight());
      if (resolved == ImageResizeOutputFormat.JPEG) {
        builder.outputQuality(resolveJpegQuality(request.getJpegQuality()));
      }
      builder.outputFormat(resolved.getThumbnailFormat()).toFile(outputTemp.toFile());

      String downloadName = buildDownloadFilename(
          multipart.getOriginalFilename(),
          resolved.getFileExtension(),
          "converted");
      ImageFileOutputDto result = new ImageFileOutputDto(outputTemp, resolved.getMediaType(), downloadName);
      outputDelivered = true;
      return result;
    } catch (ImageProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Failed to convert image", exception);
      throw new ImageProcessingException(
          "IMAGE_CONVERT_FAILED",
          "Não foi possível converter a imagem. Verifique se o arquivo é uma imagem válida.",
          exception);
    } finally {
      deleteQuietly(inputTemp);
      if (!outputDelivered) {
        deleteQuietly(outputTemp);
      }
    }
  }

  private static BufferedImage flattenTransparency(BufferedImage source) {
    if (!source.getColorModel().hasAlpha()) return source;
    BufferedImage opaque = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    var graphics = opaque.createGraphics();
    try {
      graphics.setColor(java.awt.Color.WHITE);
      graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
      graphics.drawImage(source, 0, 0, null);
    } finally { graphics.dispose(); }
    return opaque;
  }

  private BufferedImage readBoundedImage(Path input) throws IOException {
    try (var stream = ImageIO.createImageInputStream(input.toFile())) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw new ImageProcessingException("IMAGE_READ_FAILED", "Formato de imagem inválido ou não suportado.");
      var reader = readers.next();
      try {
        reader.setInput(stream, true, true);
        long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
        if (pixels <= 0 || pixels > imageUsageProperties.getMaxInputPixels()) {
          throw new ImageProcessingException("IMAGE_PIXEL_LIMIT", "A imagem excede o limite de pixels permitido.");
        }
        return reader.read(0);
      } finally { reader.dispose(); }
    }
  }

  /**
   * Escala máxima permitida sem ampliar ({@code <= 1}): encaixa dentro de maxWidth/maxHeight mantendo proporção.
   */
  private static double computeDownscaleOnlyScale(
      int srcW,
      int srcH,
      Integer maxWidth,
      Integer maxHeight
  ) {
    double scaleW = Objects.nonNull(maxWidth) ? maxWidth / (double) srcW : Double.POSITIVE_INFINITY;
    double scaleH = Objects.nonNull(maxHeight) ? maxHeight / (double) srcH : Double.POSITIVE_INFINITY;
    return Math.min(1.0, Math.min(scaleW, scaleH));
  }

  private float resolveJpegQuality(Integer requestQuality) {
    if (Objects.nonNull(requestQuality)) {
      return requestQuality / 100f;
    }
    float def = imageUsageProperties.getDefaultJpegOutputQuality();
    if (def < 0.01f) {
      return 0.01f;
    }
    if (def > 1f) {
      return 1f;
    }
    return def;
  }

  private static ImageResizeOutputFormat resolveOutputFormat(
      ImageResizeOutputFormat requested,
      MultipartFile file
  ) {
    if (Objects.isNull(requested) || requested.isAuto()) {
      return inferFromFilename(file.getOriginalFilename());
    }
    return requested;
  }

  private static ImageResizeOutputFormat inferFromFilename(String originalFilename) {
    String ext = fileExtension(originalFilename);
    if ("jpg".equals(ext) || "jpeg".equals(ext)) {
      return ImageResizeOutputFormat.JPEG;
    }
    if ("png".equals(ext)) {
      return ImageResizeOutputFormat.PNG;
    }
    if ("webp".equals(ext)) {
      return ImageResizeOutputFormat.PNG;
    }
    return ImageResizeOutputFormat.PNG;
  }

  private static String extensionSuffixFromOriginalName(String originalFilename) {
    String extension = fileExtension(originalFilename);
    if (extension.isEmpty()) {
      return ".bin";
    }
    return "." + extension;
  }

  private static String fileExtension(String originalFilename) {
    if (Objects.isNull(originalFilename) || originalFilename.isBlank()) {
      return "";
    }
    String normalized = originalFilename.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    String base = slash >= 0 ? normalized.substring(slash + 1) : normalized;
    int dot = base.lastIndexOf('.');
    if (dot <= 0 || dot == base.length() - 1) {
      return "";
    }
    return base.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  private static String buildDownloadFilename(String originalFilename, String outExtension, String operationSuffix) {
    String stem = stemWithoutExtension(originalFilename);
    if (stem.isEmpty()) {
      stem = "image";
    }
    return stem + "-" + operationSuffix + "." + outExtension;
  }

  private static String stemWithoutExtension(String originalFilename) {
    String base = baseFilename(originalFilename);
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      return base.substring(0, dot);
    }
    return base;
  }

  private static String baseFilename(String originalFilename) {
    if (Objects.isNull(originalFilename) || originalFilename.isBlank()) {
      return "";
    }
    String normalized = originalFilename.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
    return name.isBlank() ? "" : name;
  }

  static void deleteQuietly(Path path) {
    if (Objects.isNull(path)) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      log.warn("Could not delete temp file {}", path, exception);
    }
  }
}
