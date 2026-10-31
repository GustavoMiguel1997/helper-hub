package io.helperhub.api.image.validation;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * Regras de upload para processamento de imagem. O limite em bytes segue o mesmo teto configurado
 * em {@code spring.servlet.multipart.max-file-size}.
 */
public final class ImageUploadSpecifications {

  public static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      "jpg", "jpeg", "png", "webp"
  );

  private ImageUploadSpecifications() {
  }

  public static Optional<String> validateUploadedFile(MultipartFile file) {
    if (Objects.isNull(file) || file.isEmpty()) {
      return Optional.empty();
    }
    if (file.getSize() > MAX_UPLOAD_BYTES) {
      return Optional.of("O arquivo excede o tamanho máximo permitido.");
    }
    String extension = fileExtension(file.getOriginalFilename());
    if (extension.isEmpty()) {
      return Optional.of("O nome do arquivo deve incluir uma extensão suportada (ex.: .jpg, .png).");
    }
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      return Optional.of("A extensão do arquivo não é suportada para processamento de imagem.");
    }
    String contentType = file.getContentType();
    if (Objects.nonNull(contentType) && !contentType.isBlank()) {
      String lower = contentType.toLowerCase(Locale.ROOT).trim();
      if (!lower.startsWith("image/")) {
        return Optional.of("O tipo de conteúdo (Content-Type) declarado deve ser uma imagem.");
      }
    }
    return Optional.empty();
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
}
