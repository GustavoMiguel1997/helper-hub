package io.helperhub.api.image.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helperhub.api.image.config.ImageUsageProperties;
import io.helperhub.api.image.dto.ImageFileOutputDto;
import io.helperhub.api.image.dto.ImageRemoveBackgroundRequest;
import io.helperhub.api.image.exception.ImageProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ImageRemoveBackgroundService {

  private static final Logger log = LoggerFactory.getLogger(ImageRemoveBackgroundService.class);

  private static final String REMOVE_BG_URL = "https://api.remove.bg/v1.0/removebg";

  private static final ObjectMapper JSON = new ObjectMapper();

  private final ImageUsageProperties imageUsageProperties;
  private final RestClient removeBgClient;

  public ImageRemoveBackgroundService(ImageUsageProperties imageUsageProperties) {
    this.imageUsageProperties = imageUsageProperties;
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(java.time.Duration.ofSeconds(10));
    factory.setReadTimeout(java.time.Duration.ofSeconds(120));
    this.removeBgClient = RestClient.builder().requestFactory(factory).build();
  }

  public ImageFileOutputDto removeBackground(ImageRemoveBackgroundRequest request) {
    String apiKey = imageUsageProperties.getRemoveBgApiKey();
    if (Objects.isNull(apiKey) || apiKey.isBlank()) {
      throw new ImageProcessingException(
          "REMBG_NOT_CONFIGURED",
          "Configure helperhub.image.remove-bg-api-key com uma chave válida da API remove.bg.");
    }

    Path outputTemp = null;
    boolean outputDelivered = false;
    try {
      outputTemp = Files.createTempFile("helperhub-image-nobg-", ".png");

      MultipartBodyBuilder multipart = new MultipartBodyBuilder();
      String filename = safeFilename(request.getFile().getOriginalFilename());
      multipart.part("image_file", request.getFile().getResource())
          .filename(filename)
          .contentType(resolvePartContentType(request));
      multipart.part("size", "auto");

      byte[] pngBytes = removeBgClient.post()
          .uri(REMOVE_BG_URL)
          .header("X-Api-Key", apiKey.trim())
          .body(multipart.build())
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw mapRemoveBgError(res.getStatusCode().value(), readBodyString(res.getBody()));
          })
          .body(byte[].class);
      if (Objects.isNull(pngBytes) || pngBytes.length == 0) {
        throw new ImageProcessingException(
            "EMPTY_RESULT",
            "O serviço de remoção de fundo devolveu uma resposta vazia.");
      }
      Files.write(outputTemp, pngBytes);

      String downloadName = buildDownloadFilename(request.getFile().getOriginalFilename());
      ImageFileOutputDto result = new ImageFileOutputDto(outputTemp, MediaType.IMAGE_PNG_VALUE, downloadName);
      outputDelivered = true;
      return result;
    } catch (ImageProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Failed to remove image background", exception);
      throw new ImageProcessingException(
          "IMAGE_PROCESSING_FAILED",
          "Não foi possível remover o fundo da imagem.",
          exception);
    } finally {
      if (!outputDelivered) {
        deleteQuietly(outputTemp);
      }
    }
  }

  private static ImageProcessingException mapRemoveBgError(int statusCode, String body) {
    String message = extractRemoveBgMessage(body);
    if (Objects.isNull(message) || message.isBlank()) {
      message = "O serviço de remoção de fundo recusou a requisição (HTTP " + statusCode + ").";
    }
    String code = statusCode == 402 ? "REMBG_CREDITS_EXHAUSTED" : "REMBG_REQUEST_FAILED";
    return new ImageProcessingException(code, message);
  }

  private static String extractRemoveBgMessage(String jsonBody) {
    if (Objects.isNull(jsonBody) || jsonBody.isBlank()) {
      return null;
    }
    try {
      JsonNode root = JSON.readTree(jsonBody);
      JsonNode errors = root.path("errors");
      if (errors.isArray() && errors.size() > 0) {
        JsonNode first = errors.get(0);
        if (first.hasNonNull("title")) {
          return first.get("title").asText();
        }
      }
    } catch (Exception ignored) {
      // fall through
    }
    return null;
  }

  private static String readBodyString(InputStream body) {
    if (Objects.isNull(body)) {
      return "";
    }
    try {
      return new String(body.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      return "";
    }
  }

  private static MediaType resolvePartContentType(ImageRemoveBackgroundRequest request) {
    String declared = request.getFile().getContentType();
    if (Objects.nonNull(declared) && !declared.isBlank()) {
      try {
        return MediaType.parseMediaType(declared);
      } catch (Exception ignored) {
        // use default below
      }
    }
    return MediaType.APPLICATION_OCTET_STREAM;
  }

  private static String safeFilename(String originalFilename) {
    String base = baseFilename(originalFilename);
    if (base.isEmpty()) {
      return "image.jpg";
    }
    return base;
  }

  private static String buildDownloadFilename(String originalFilename) {
    String stem = stemWithoutExtension(originalFilename);
    if (stem.isEmpty()) {
      stem = "image";
    }
    return stem + "-nobg.png";
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

  private static void deleteQuietly(Path path) {
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
