package io.helperhub.api.audio.controller;

import io.helperhub.api.audio.dto.AudioConvertRequest;
import io.helperhub.api.audio.dto.AudioCutRequest;
import io.helperhub.api.audio.dto.AudioFileOutputDto;
import io.helperhub.api.audio.service.AudioService;
import io.helperhub.api.common.support.DeletingFileStreamResourceFactory;
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
@RequestMapping("/api/audio")
@Tag(name = "Audio", description = "Operações de áudio")
public class AudioController {

  private final AudioService audioService;

  public AudioController(AudioService audioService) {
    this.audioService = audioService;
  }

  @Operation(summary = "Cortar áudio", description = "Recebe um arquivo de áudio, extrai o trecho entre startMs e endMs (milissegundos) e devolve o arquivo cortado.")
  @ApiResponse(responseCode = "200", description = "Arquivo de áudio cortado", content = @Content(mediaType = "application/octet-stream"))
  @ApiResponse(responseCode = "400", description = "Dados inválidos")
  @ApiResponse(responseCode = "422", description = "Falha ao processar com FFmpeg")
  @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
  @PostMapping(value = "/cut", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> cutAudio(@Valid @ModelAttribute AudioCutRequest request) {
    AudioFileOutputDto result = audioService.cutAudio(request);
    return attachmentResponse(result);
  }

  @Operation(
      summary = "Converter formato de áudio",
      description = "Recebe um arquivo de áudio e gera uma nova saída no formato escolhido (reencode da faixa de áudio principal)."
  )
  @ApiResponse(responseCode = "200", description = "Arquivo convertido", content = @Content(mediaType = "application/octet-stream"))
  @ApiResponse(responseCode = "400", description = "Dados inválidos")
  @ApiResponse(responseCode = "422", description = "Falha ao processar com FFmpeg")
  @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
  @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> convertAudio(@Valid @ModelAttribute AudioConvertRequest request) {
    AudioFileOutputDto result = audioService.convertAudio(request);
    return attachmentResponse(result);
  }

  @Operation(summary = "Extrair áudio de vídeo", description = "Extrai e converte a primeira faixa de áudio. Requer um arquivo com vídeo e áudio.")
  @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Resource> extractAudio(@Valid @ModelAttribute AudioConvertRequest request) {
    return attachmentResponse(audioService.extractAudio(request));
  }

  private static ResponseEntity<Resource> attachmentResponse(AudioFileOutputDto result) {
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
