package io.helperhub.api.image.controller;

import io.helperhub.api.common.support.DeletingFileStreamResourceFactory;
import io.helperhub.api.image.dto.ImageConvertRequest;
import io.helperhub.api.image.dto.ImageCompressRequest;
import io.helperhub.api.image.dto.ImageFileOutputDto;
import io.helperhub.api.image.dto.ImageRemoveBackgroundRequest;
import io.helperhub.api.image.dto.ImageResizeRequest;
import io.helperhub.api.image.service.ImageRemoveBackgroundService;
import io.helperhub.api.image.service.ImageResizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image")
@Tag(name = "Imagem", description = "Operações de imagem")
public class ImageController {

  private final ImageRemoveBackgroundService imageRemoveBackgroundService;
  private final ImageResizeService imageResizeService;

  public ImageController(
      ImageRemoveBackgroundService imageRemoveBackgroundService,
      ImageResizeService imageResizeService
  ) {
    this.imageRemoveBackgroundService = imageRemoveBackgroundService;
    this.imageResizeService = imageResizeService;
  }

  @Operation(
      summary = "Remover fundo da imagem",
      description = "Envia uma imagem (JPG, PNG ou WebP) e devolve um PNG com fundo removido (alpha). "
          + "Requer chave da API remove.bg em helperhub.image.remove-bg-api-key."
  )
  @ApiResponse(responseCode = "200", description = "PNG com transparência", content = @Content(mediaType = "image/png"))
  @ApiResponse(responseCode = "400", description = "Dados inválidos")
  @ApiResponse(responseCode = "422", description = "Falha ao processar com o serviço externo")
  @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
  @ApiResponse(responseCode = "503", description = "API de remoção de fundo não configurada")
  @PostMapping(value = "/remove-background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> removeBackground(@Valid @ModelAttribute ImageRemoveBackgroundRequest request) {
    ImageFileOutputDto result = imageRemoveBackgroundService.removeBackground(request);
    return attachmentResponse(result);
  }

  @Operation(
      summary = "Redimensionar imagem",
      description = "Envia uma imagem (JPG, PNG ou WebP). Use maxWidth e/ou maxHeight; a proporção é mantida e imagens "
          + "menores que o alvo não são ampliadas. Formato de saída: outputFormat AUTO (segue o tipo de entrada; WebP vira PNG), "
          + "JPEG ou PNG. jpegQuality (1–100) só vale para JPEG."
  )
  @ApiResponse(responseCode = "200", description = "Imagem redimensionada")
  @ApiResponse(responseCode = "400", description = "Dados inválidos")
  @ApiResponse(responseCode = "422", description = "Falha ao processar a imagem")
  @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
  @PostMapping(value = "/resize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> resize(@Valid @ModelAttribute ImageResizeRequest request) {
    ImageFileOutputDto result = imageResizeService.resize(request);
    return attachmentResponse(result);
  }

  @Operation(
      summary = "Converter formato da imagem",
      description = "Envia uma imagem (JPG, PNG ou WebP) e converte para JPEG ou PNG sem redimensionar. "
          + "jpegQuality (1–100) só vale para JPEG."
  )
  @ApiResponse(responseCode = "200", description = "Imagem convertida")
  @ApiResponse(responseCode = "400", description = "Dados inválidos")
  @ApiResponse(responseCode = "422", description = "Falha ao processar a imagem")
  @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
  @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> convert(@Valid @ModelAttribute ImageConvertRequest request) {
    ImageFileOutputDto result = imageResizeService.convert(request);
    return attachmentResponse(result);
  }

  @Operation(summary = "Comprimir imagem", description = "Em JPEG, targetBytes define um teto em bytes e allowResize permite reduzir as dimensões.")
  @PostMapping(value = "/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> compress(@Valid @ModelAttribute ImageCompressRequest request) {
    return attachmentResponse(imageResizeService.compress(request));
  }

  static ResponseEntity<Resource> attachmentResponse(ImageFileOutputDto result) {
    Path path = result.tempOutputFile();
    try {
      long length = Files.size(path);
      Resource resource = DeletingFileStreamResourceFactory.createDeletingOnClose(path);
      ContentDisposition disposition = ContentDisposition.attachment()
          .filename(result.downloadFilename(), StandardCharsets.UTF_8)
          .build();
      return ResponseEntity.ok()
          .contentLength(length)
          .contentType(MediaType.parseMediaType(result.contentType()))
          .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
          .body(resource);
    } catch (IOException exception) {
      throw new UncheckedIOException("Não foi possível enviar o arquivo.", exception);
    }
  }
}
