package io.helperhub.api.video.service;

import io.helperhub.api.video.config.VideoUsageProperties;
import io.helperhub.api.video.domain.VideoOutputFormat;
import io.helperhub.api.video.dto.VideoFileOutputDto;
import io.helperhub.api.video.dto.VideoYoutubeDownloadRequest;
import io.helperhub.api.video.exception.VideoProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VideoService {

  private static final Logger log = LoggerFactory.getLogger(VideoService.class);

  private final VideoUsageProperties videoUsageProperties;

  public VideoService(VideoUsageProperties videoUsageProperties) {
    this.videoUsageProperties = videoUsageProperties;
  }

  public VideoFileOutputDto downloadFromYoutube(VideoYoutubeDownloadRequest request) {
    String url = normalizeYoutubeUrl(request.getUrl());
    VideoOutputFormat format = Objects.nonNull(request.getOutputFormat())
        ? request.getOutputFormat()
        : VideoOutputFormat.MP4;

    boolean outputDelivered = false;
    Path outputPath = null;
    Path work = null;
    try {
      work = Files.createTempDirectory("helperhub-video-job-");
      String outputTemplate = work.resolve("video.%(ext)s").toString();
      List<String> command = buildYtDlpCommand(url, format, outputTemplate);
      runCommand(command, work);
      Path downloaded = work.resolve("video." + format.getFileExtension());
      if (!Files.isRegularFile(downloaded) || Files.size(downloaded) == 0) {
        throw new VideoProcessingException("YOUTUBE_OUTPUT_NOT_FOUND", "Vídeo indisponível no formato ou nos limites solicitados.");
      }
      if (Files.size(downloaded) > videoUsageProperties.getMaxDownloadBytes()) {
        throw new VideoProcessingException("YOUTUBE_SIZE_LIMIT", "O vídeo excede o tamanho máximo permitido.");
      }
      outputPath = Files.createTempFile("helperhub-video-out-", "." + format.getFileExtension());
      Files.move(downloaded, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      String downloadFilename = "youtube-video." + format.getFileExtension();
      VideoFileOutputDto result = new VideoFileOutputDto(outputPath, format.getMediaType(), downloadFilename);
      outputDelivered = true;
      return result;
    } catch (VideoProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Failed to download YouTube video", exception);
      throw new VideoProcessingException(
          "YOUTUBE_DOWNLOAD_FAILED",
          "Não foi possível baixar o vídeo do YouTube.",
          exception);
    } finally {
      deleteTree(work);
      if (!outputDelivered) {
        deleteQuietly(outputPath);
      }
    }
  }

  private List<String> buildYtDlpCommand(String url, VideoOutputFormat format, String outputTemplate) {
    List<String> command = new ArrayList<>();
    command.add(videoUsageProperties.getYtDlpCommand());
    command.add("--no-playlist");
    command.add("--no-progress");
    command.add("--no-warnings");
    command.addAll(List.of("--ignore-config", "--no-cache-dir", "--no-simulate",
        "--js-runtimes", "node", "--socket-timeout", "30", "--retries", "2", "--fragment-retries", "2",
        "--max-filesize", Long.toString(videoUsageProperties.getMaxDownloadBytes()),
        "--match-filters", "!is_live & duration <= " + videoUsageProperties.getMaxVideoDurationSeconds()));
    command.add("-o");
    command.add(outputTemplate);
    command.add("--merge-output-format");
    command.add(format.getFileExtension());
    command.add("-f");
    command.add(selectFormatExpression(format));
    command.add("--");
    command.add(url);
    return command;
  }

  private static String selectFormatExpression(VideoOutputFormat format) {
    if (format == VideoOutputFormat.WEBM) {
      return "bestvideo[ext=webm][height<=1080]+bestaudio[ext=webm]/best[ext=webm][height<=1080]";
    }
    return "bestvideo[ext=mp4][height<=1080]+bestaudio[ext=m4a]/best[ext=mp4][height<=1080]";
  }

  // Output is discarded so a full pipe cannot block timeout enforcement.
  void runCommand(List<String> command, Path work) {
    Process process;
    try {
      process = new ProcessBuilder(command)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD).start();
    } catch (IOException exception) {
      throw new VideoProcessingException("YTDLP_NOT_AVAILABLE", "yt-dlp não está disponível. Configure helperhub.video.yt-dlp-command.", exception);
    }
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(1, videoUsageProperties.getMaxDownloadDurationSeconds()));
    try {
      process.getOutputStream().close();
      while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
        if (System.nanoTime() >= deadline) {
          throw new VideoProcessingException("YOUTUBE_DOWNLOAD_TIMEOUT", "Tempo limite excedido ao baixar vídeo.");
        }
        if (directoryBytes(work) > videoUsageProperties.getMaxDownloadBytes()) {
          throw new VideoProcessingException("YOUTUBE_SIZE_LIMIT", "O download excede o espaço temporário permitido.");
        }
      }
      if (process.exitValue() != 0) {
        throw new VideoProcessingException("YOUTUBE_DOWNLOAD_FAILED", "Não foi possível baixar o vídeo. Verifique URL, disponibilidade e formato.");
      }
    } catch (IOException exception) {
      throw new VideoProcessingException("YOUTUBE_DOWNLOAD_FAILED", "Falha ao controlar o download.", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new VideoProcessingException("YOUTUBE_DOWNLOAD_INTERRUPTED", "Download interrompido.", exception);
    } finally {
      terminate(process);
    }
  }

  private static long directoryBytes(Path directory) throws IOException {
    try (var paths = Files.walk(directory)) {
      long total = 0;
      for (Path path : paths.filter(Files::isRegularFile).toList()) {
        try { total += Files.size(path); } catch (java.nio.file.NoSuchFileException ignored) { }
      }
      return total;
    }
  }

  static void terminate(Process process) {
    var children = process.descendants().toList();
    children.forEach(ProcessHandle::destroyForcibly);
    if (process.isAlive()) process.destroyForcibly();
    boolean interrupted = Thread.interrupted();
    try {
      process.waitFor(5, TimeUnit.SECONDS);
      for (ProcessHandle child : children) {
        try { child.onExit().get(5, TimeUnit.SECONDS); }
        catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ignored) { }
      }
    } catch (InterruptedException exception) {
      interrupted = true;
    } finally {
      if (interrupted) Thread.currentThread().interrupt();
    }
  }

  static String normalizeYoutubeUrl(String raw) {
    try {
      URI uri = URI.create(raw == null ? "" : raw.trim());
      String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || !Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be").contains(host)
          || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) throw new IllegalArgumentException();
      String id = null;
      String path = uri.getPath();
      if (host.equals("youtu.be") && path.startsWith("/")) id = path.substring(1);
      else if (path.startsWith("/shorts/")) id = path.substring(8);
      else if (path.equals("/watch") && uri.getRawQuery() != null) {
        for (String parameter : uri.getRawQuery().split("&")) {
          String[] pair = parameter.split("=", 2);
          if (pair.length == 2 && pair[0].equals("v")) id = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
        }
      }
      if (id == null || !id.matches("[A-Za-z0-9_-]{11}")) throw new IllegalArgumentException();
      return "https://www.youtube.com/watch?v=" + id;
    } catch (IllegalArgumentException exception) {
      throw new VideoProcessingException("INVALID_YOUTUBE_URL", "Envie uma URL HTTPS válida de um vídeo do YouTube.");
    }
  }

  static void deleteTree(Path root) {
    if (root == null) return;
    try (var paths = Files.walk(root)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) deleteQuietly(path);
    } catch (IOException exception) {
      log.warn("Não foi possível limpar temporários em {}", root, exception);
    }
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
