package io.helperhub.api.audio.validation;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * Regras de upload para processamento de áudio. O limite de tamanho em bytes é aplicado pelo
 * Spring via {@code spring.servlet.multipart.max-file-size}; este tipo documenta o mesmo teto para
 * checagens defensivas e mensagens.
 */
public final class AudioUploadSpecifications {

  public static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "webm", "mp4", "mpeg", "mpg",
      "wma", "aiff", "aif", "caf", "3gp", "mkv", "mov", "avi", "wmv"
  );

  private static final Set<String> ADDITIONAL_ALLOWED_MEDIA_TYPES = Set.of(
      "application/ogg",
      "application/octet-stream",
      "application/mp4"
  );

  private AudioUploadSpecifications() {
  }

  /**
   * Valida nome/extensão e, se presente, o Content-Type declarado. Não inspeciona o binário
   * (magic bytes); isso continua sendo responsabilidade do FFmpeg em tempo de processamento.
   */
  public static Optional<String> validateUploadedFile(MultipartFile file) {
    if (Objects.isNull(file) || file.isEmpty()) {
      return Optional.empty();
    }
    if (file.getSize() > MAX_UPLOAD_BYTES) {
      return Optional.of("O arquivo excede o tamanho máximo permitido.");
    }
    String extension = fileExtension(file.getOriginalFilename());
    if (extension.isEmpty()) {
      return Optional.of("O nome do arquivo deve incluir uma extensão suportada (ex.: .mp3, .wav).");
    }
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      return Optional.of("A extensão do arquivo não é suportada para processamento de áudio.");
    }
    String contentType = file.getContentType();
    if (Objects.nonNull(contentType) && !contentType.isBlank()) {
      if (!isAllowedDeclaredContentType(contentType)) {
        return Optional.of("O tipo de conteúdo (Content-Type) declarado não é permitido para áudio.");
      }
    }
    return Optional.empty();
  }

  private static boolean isAllowedDeclaredContentType(String contentType) {
    String lower = contentType.toLowerCase(Locale.ROOT).trim();
    if (lower.startsWith("audio/") || lower.startsWith("video/")) {
      return true;
    }
    return ADDITIONAL_ALLOWED_MEDIA_TYPES.contains(lower);
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
