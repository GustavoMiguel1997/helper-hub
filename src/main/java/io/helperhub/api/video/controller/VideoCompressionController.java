package io.helperhub.api.video.controller;

import io.helperhub.api.video.dto.VideoCompressRequest;
import io.helperhub.api.video.service.VideoCompressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Video", description = "Operações de vídeo")
public class VideoCompressionController {
  private final VideoCompressionService compression;
  public VideoCompressionController(VideoCompressionService compression) {
    this.compression = compression;
  }

  @Operation(summary = "Comprimir vídeo", description = "Recebe um vídeo de até 50 MB e retorna MP4 H.264/AAC. "
      + "resolution 480/720/1080, quality HIGH/BALANCED/SMALL. Limite padrão de 5 minutos.")
  @PostMapping(value = "/api/video/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> compress(@Valid @ModelAttribute VideoCompressRequest request) {
    return VideoController.attachmentResponse(compression.compress(request));
  }
}
