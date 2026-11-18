package io.helperhub.api.video.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class VideoCompressRequest {
  public static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;
  private static final Set<String> EXTENSIONS = Set.of("mp4","webm","mkv","mov","avi","wmv","mpeg","mpg","3gp");
  public enum Quality {
    HIGH(23), BALANCED(28), SMALL(32);
    private final int crf;
    Quality(int crf) { this.crf = crf; }
    public int getCrf() { return crf; }
  }
  @NotNull(message = "Selecione um vídeo.")
  private MultipartFile file;
  @NotNull
  private Integer resolution = 720;
  @NotNull
  private Quality quality = Quality.BALANCED;

  @AssertTrue(message = "Escolha 480, 720 ou 1080 para a resolução.")
  public boolean isResolutionValid() {
    return resolution == null || Set.of(480,720,1080).contains(resolution);
  }

  @AssertTrue(message = "Envie um vídeo não vazio de até 50 MB, com extensão e tipo de conteúdo aceitos.")
  public boolean isUploadValid() {
    if (file == null) return true;
    if (file.isEmpty() || file.getSize() > MAX_UPLOAD_BYTES) return false;
    String name = file.getOriginalFilename();
    if (name == null || !name.matches("(?is).+\\.[a-z0-9]+")) return false;
    String extension = name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
    String type = file.getContentType();
    return EXTENSIONS.contains(extension) && (type == null || type.isBlank()
        || type.toLowerCase(Locale.ROOT).startsWith("video/")
        || type.equalsIgnoreCase("application/octet-stream")
        || type.equalsIgnoreCase("application/mp4"));
  }
}
