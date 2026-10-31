package io.helperhub.api.video.controller;

import io.helperhub.api.common.support.DeletingFileStreamResourceFactory;
import io.helperhub.api.video.dto.VideoFileOutputDto;
import io.helperhub.api.video.dto.VideoYoutubeDownloadRequest;
import io.helperhub.api.video.service.VideoService;
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
@RequestMapping("/api/video")
@Tag(name = "Video", description = "Operações de vídeo")
public class VideoController {

  private final VideoService videoService;

  public VideoController(VideoService videoService) {
    this.videoService = videoService;
  }

  @Operation(
      summary = "Baixar vídeo do YouTube",
      description = "Baixa um vídeo do YouTube e retorna o arquivo no formato escolhido (MP4 ou WEBM)."
  )
  @ApiResponse(responseCode = "200", description = "Vídeo baixado com sucesso", content = @Content(mediaType = "application/octet-stream"))
  @ApiResponse(responseCode = "400", description = "Dados inválidos")
  @ApiResponse(responseCode = "422", description = "Falha ao baixar/processar vídeo")
  @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
  @PostMapping("/youtube-download")
  public ResponseEntity<Resource> youtubeDownload(@Valid @ModelAttribute VideoYoutubeDownloadRequest request) {
    VideoFileOutputDto result = videoService.downloadFromYoutube(request);
    return attachmentResponse(result);
  }

  private static ResponseEntity<Resource> attachmentResponse(VideoFileOutputDto result) {
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
