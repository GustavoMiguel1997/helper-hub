package io.helperhub.api.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helperhub.api.video.config.VideoUsageProperties;
import io.helperhub.api.video.dto.VideoCompressRequest;
import io.helperhub.api.video.dto.VideoFileOutputDto;
import io.helperhub.api.video.exception.VideoProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class VideoCompressionService {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String FORMATS = "mov,matroska,webm,avi,asf,mpeg,mpegts";
  private static final long MAX_PROBE_BYTES = 1024 * 1024;
  private final VideoUsageProperties limits;

  public VideoCompressionService(VideoUsageProperties limits) { this.limits = limits; }

  public VideoFileOutputDto compress(VideoCompressRequest request) {
    Path work = null;
    Path deliveredFile = null;
    boolean delivered = false;
    try {
      work = Files.createTempDirectory("helperhub-video-compress-");
      Path input = work.resolve("input.bin");
      Path output = work.resolve("output.mp4");
      request.getFile().transferTo(input);
      MediaInfo media = probe(input,work.resolve("input.json"));
      int shortSide = request.getResolution();
      int longSide = shortSide == 480 ? 854 : shortSide*16/9;
      String scale = "scale=w='min(iw,if(gte(iw,ih)," + longSide + "," + shortSide + "))'"
          + ":h='min(ih,if(gte(iw,ih)," + shortSide + "," + longSide + "))'"
          + ":force_original_aspect_ratio=decrease:force_divisible_by=2,fps=30";
      var command = new ArrayList<>(List.of("ffmpeg","-nostdin","-hide_banner","-loglevel","error",
          "-y","-max_alloc","67108864","-filter_threads","1","-threads","1",
          "-protocol_whitelist","file","-format_whitelist",FORMATS,
          "-probesize","5000000","-analyzeduration","5000000","-i",input.toString(),
          "-map","0:" + media.videoIndex(),"-map","0:a:0?","-map_metadata","-1","-map_chapters","-1",
          "-vf",scale,"-c:v","libx264","-preset","veryfast","-crf",Integer.toString(request.getQuality().getCrf()),
          "-pix_fmt","yuv420p","-threads","1","-c:a","aac","-b:a","96k","-ac","2",
          "-maxrate","2500k","-bufsize","5000k","-t",Long.toString(limits.getMaxCompressDurationSeconds()),
          "-movflags","+faststart",output.toString()));
      run(command,null,output,limits.getMaxCompressOutputBytes(),limits.getMaxCompressProcessingSeconds());
      MediaInfo encoded = probe(output,work.resolve("output.json"));
      if (Math.abs(encoded.duration()-media.duration()) > 0.5) {
        throw new VideoProcessingException("VIDEO_INCOMPLETE", "O vídeo não pôde ser processado por inteiro.");
      }
      deliveredFile = Files.createTempFile("helperhub-compressed-video-", ".mp4");
      Files.move(output,deliveredFile,StandardCopyOption.REPLACE_EXISTING);
      String original = request.getFile().getOriginalFilename().replace('\\','/');
      String base = original.substring(original.lastIndexOf('/')+1);
      String stem = base.substring(0,base.lastIndexOf('.')).replaceAll("[\\p{Cntrl}\\\"<>|:*?]", "_");
      if (stem.isBlank()) stem = "video";
      stem = stem.substring(0,Math.min(100,stem.length()));
      var result = new VideoFileOutputDto(deliveredFile,"video/mp4",stem+"-compressed.mp4");
      delivered = true;
      return result;
    } catch (VideoProcessingException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new VideoProcessingException("VIDEO_PROCESSING_FAILED", "Não foi possível preparar o vídeo.",exception);
    } finally {
      VideoService.deleteTree(work);
      if (!delivered && deliveredFile != null) {
        try { Files.deleteIfExists(deliveredFile); } catch (IOException ignored) {}
      }
    }
  }

  private MediaInfo probe(Path input, Path json) throws IOException {
    run(List.of("ffprobe","-v","error","-max_alloc","67108864","-threads","1",
        "-protocol_whitelist","file","-format_whitelist",FORMATS,
        "-probesize","5000000","-analyzeduration","5000000",
        "-show_entries","format=format_name,duration:stream=index,codec_type,width,height,avg_frame_rate,r_frame_rate,duration,channels:stream_disposition=attached_pic",
        "-of","json",input.toString()),json,json,MAX_PROBE_BYTES,20);
    return validateMetadata(JSON.readTree(json.toFile()));
  }

  MediaInfo validateMetadata(JsonNode metadata) {
    JsonNode streams = metadata.path("streams");
    if (!streams.isArray() || streams.isEmpty() || streams.size() > 16) {
      throw new VideoProcessingException("VIDEO_STREAM_LIMIT", "O arquivo possui quantidade de faixas inválida.");
    }
    Set<String> allowedFormats = Set.of("mov","mp4","m4a","3gp","3g2","mj2","matroska","webm","avi","asf","mpeg","mpegts");
    String formatName = metadata.path("format").path("format_name").asText();
    if (List.of(formatName.split(",")).stream().noneMatch(allowedFormats::contains)) {
      throw new VideoProcessingException("VIDEO_FORMAT_INVALID", "O conteúdo não é um formato de vídeo aceito.");
    }
    JsonNode video = null;
    double duration = metadata.path("format").path("duration").asDouble(Double.NaN);
    for (JsonNode stream : streams) {
      if (video == null && "video".equals(stream.path("codec_type").asText())
          && stream.path("disposition").path("attached_pic").asInt() == 0) video = stream;
      double streamDuration = stream.path("duration").asDouble(0);
      if (Double.isFinite(streamDuration) && (Double.isNaN(duration) || streamDuration > duration)) duration = streamDuration;
      int channels = stream.path("channels").asInt();
      if ("audio".equals(stream.path("codec_type").asText()) && (channels < 1 || channels > 8)) {
        throw new VideoProcessingException("VIDEO_AUDIO_LIMIT", "O vídeo excede o limite de canais de áudio.");
      }
    }
    if (video == null) throw new VideoProcessingException("NO_VIDEO_STREAM", "O arquivo não contém uma faixa de vídeo válida.");
    if (!Double.isFinite(duration) || duration <= 0 || duration > limits.getMaxCompressDurationSeconds()) {
      throw new VideoProcessingException("VIDEO_DURATION_LIMIT", "Duração inválida ou acima do limite de compressão.");
    }
    int width = video.path("width").asInt();
    int height = video.path("height").asInt();
    if (width < 2 || height < 2 || width > 4096 || height > 4096 || (long) width*height > 8294400
        || Math.max(width,height)/(double)Math.min(width,height) > 256) {
      throw new VideoProcessingException("VIDEO_DIMENSION_LIMIT", "A resolução ou a proporção excede os limites de entrada.");
    }
    double fps = frameRate(video.path("avg_frame_rate").asText());
    if (fps == 0) fps = frameRate(video.path("r_frame_rate").asText());
    if (!Double.isFinite(fps) || fps <= 0 || fps > 60.01) {
      throw new VideoProcessingException("VIDEO_FPS_LIMIT", "A taxa de quadros é inválida ou ultrapassa 60 fps.");
    }
    return new MediaInfo(video.path("index").asInt(),duration);
  }

  private static double frameRate(String value) {
    try {
      String[] terms = value.split("/");
      if (terms.length != 2) return 0;
      double denominator = Double.parseDouble(terms[1]);
      return denominator > 0 ? Double.parseDouble(terms[0])/denominator : 0;
    } catch (NumberFormatException exception) { return 0; }
  }

  void run(List<String> command, Path stdout, Path boundedFile, long maxBytes, long timeoutSeconds) {
    Process process;
    try {
      var builder = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
      builder.redirectOutput(stdout == null ? ProcessBuilder.Redirect.DISCARD : ProcessBuilder.Redirect.to(stdout.toFile()));
      process = builder.start();
    } catch (IOException exception) {
      throw new VideoProcessingException("VIDEO_TOOLS_UNAVAILABLE", "O processamento de vídeo não está disponível neste servidor.",exception);
    }
    long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(timeoutSeconds);
    try {
      process.getOutputStream().close();
      while (!process.waitFor(100,TimeUnit.MILLISECONDS)) {
        checkSize(boundedFile,maxBytes);
        if (System.nanoTime() >= deadline) {
          throw new VideoProcessingException("VIDEO_PROCESSING_TIMEOUT", "Tempo limite excedido ao processar o vídeo.");
        }
      }
      checkSize(boundedFile,maxBytes);
      if (process.exitValue() != 0 || !Files.isRegularFile(boundedFile) || Files.size(boundedFile) == 0) {
        throw new VideoProcessingException("VIDEO_PROCESSING_FAILED", "Vídeo inválido, corrompido ou incompatível com o processamento.");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new VideoProcessingException("VIDEO_PROCESSING_INTERRUPTED", "Processamento de vídeo interrompido.",exception);
    } catch (IOException exception) {
      throw new VideoProcessingException("VIDEO_PROCESSING_FAILED", "Falha ao controlar o processamento.",exception);
    } finally {
      VideoService.terminate(process);
    }
  }

  private static void checkSize(Path path, long maximum) throws IOException {
    if (Files.exists(path) && Files.size(path) > maximum) {
      throw new VideoProcessingException("VIDEO_OUTPUT_LIMIT", "O processamento excedeu o limite de espaço temporário permitido.");
    }
  }

  record MediaInfo(int videoIndex, double duration) {}
}
