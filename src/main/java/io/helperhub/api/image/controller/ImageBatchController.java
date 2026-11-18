package io.helperhub.api.image.controller;

import io.helperhub.api.image.dto.ImageBatchRequest;
import io.helperhub.api.image.service.ImageBatchService;
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
@Tag(name = "Imagem", description = "Operações de imagem")
public class ImageBatchController {
  private final ImageBatchService batchService;

  public ImageBatchController(ImageBatchService batchService) {
    this.batchService = batchService;
  }

  @Operation(summary = "Processar imagens em lote",
      description = "Aplica COMPRESS, CONVERT ou RESIZE a todos os arquivos e devolve um ZIP.")
  @PostMapping(value = "/api/image/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> batch(@Valid @ModelAttribute ImageBatchRequest request) {
    return ImageController.attachmentResponse(batchService.process(request));
  }
}
