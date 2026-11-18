package io.helperhub.api.audio.service;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import io.helperhub.api.audio.config.AudioUsageProperties;
import io.helperhub.api.audio.domain.AudioOutputFormat;
import io.helperhub.api.audio.dto.AudioConvertRequest;
import io.helperhub.api.audio.dto.AudioCutRequest;
import io.helperhub.api.audio.dto.AudioFileOutputDto;
import io.helperhub.api.audio.dto.AudioMixRequest;
import java.util.ArrayList;
import io.helperhub.api.audio.exception.AudioProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioService {

  private static final Logger log = LoggerFactory.getLogger(AudioService.class);

  private final AudioUsageProperties audioUsageProperties;

  public AudioService(AudioUsageProperties audioUsageProperties) {
    this.audioUsageProperties = audioUsageProperties;
  }

  public AudioFileOutputDto cutAudio(AudioCutRequest request) {
    long segmentMillis = request.getEndMs() - request.getStartMs();
    validateCutSegmentLength(segmentMillis);

    long durationMillis = segmentMillis;
    Path inputTemp = null;
    Path outputTemp = null;
    boolean outputDelivered = false;
    try {
      String extensionSuffix = extensionSuffixFromOriginalName(request.getFile().getOriginalFilename());
      inputTemp = Files.createTempFile("helperhub-audio-in-", extensionSuffix);
      outputTemp = Files.createTempFile("helperhub-audio-out-", extensionSuffix);
      request.getFile().transferTo(inputTemp);

      long mediaDurationMs = probeAudioDurationMillis(inputTemp);
      validateCutInterval(request.getStartMs(), request.getEndMs(), mediaDurationMs);

      runFfmpegCut(inputTemp, outputTemp, request.getStartMs(), durationMillis);

      String downloadFilename = buildCutFilename(request.getFile().getOriginalFilename());
      String contentType = resolveContentType(request.getFile());
      AudioFileOutputDto result = new AudioFileOutputDto(outputTemp, contentType, downloadFilename);
      outputDelivered = true;
      return result;
    } catch (AudioProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Failed to cut audio", exception);
      throw new AudioProcessingException("AUDIO_PROCESSING_FAILED", "Não foi possível processar o corte de áudio.", exception);
    } finally {
      deleteQuietly(inputTemp);
      if (!outputDelivered) {
        deleteQuietly(outputTemp);
      }
    }
  }

  public AudioFileOutputDto convertAudio(AudioConvertRequest request) {
    return convertAudio(request, false);
  }

  public AudioFileOutputDto extractAudio(AudioConvertRequest request) {
    return convertAudio(request, true);
  }

  public AudioFileOutputDto mixAudio(AudioMixRequest request, boolean join) {
    int count = request.getFiles().size();
    if ((join && count < 2) || (!join && count != 1)) {
      throw new AudioProcessingException("AUDIO_FILE_COUNT",
          join ? "Selecione de 2 a 5 arquivos para juntar." : "Selecione um arquivo para ajustar.");
    }
    var inputs = new ArrayList<Path>();
    var monoInputs = new ArrayList<Boolean>();
    Path output = null;
    boolean delivered = false;
    try {
      long durationMs = 0;
      for (MultipartFile file : request.getFiles()) {
        Path input = Files.createTempFile("helperhub-audio-mix-in-", ".bin");
        inputs.add(input);
        file.transferTo(input);
        var probe = runFfprobe(input);
        durationMs += audioDurationMillis(probe);
        monoInputs.add(probe.getStreams().stream()
            .filter(stream -> StreamType.AUDIO.equals(stream.getCodecType()))
            .findFirst().map(stream -> Integer.valueOf(1).equals(stream.getChannels())).orElse(false));
        if (durationMs > audioUsageProperties.getMaxEditDurationMs()) {
          throw new AudioProcessingException("AUDIO_DURATION_LIMIT",
              "A duração total excede o limite de edição de áudio do servidor.");
        }
      }
      if (request.getFadeInMs() > durationMs || request.getFadeOutMs() > durationMs) {
        throw new AudioProcessingException("AUDIO_FADE_DURATION",
            "Cada fade deve ser menor ou igual à duração total do áudio.");
      }
      AudioOutputFormat format = request.getTargetFormat();
      output = Files.createTempFile("helperhub-audio-mix-out-", "." + format.getFileExtension());
      var ffmpeg = FFmpeg.atPath();
      var filters = new StringBuilder();
      var labels = new StringBuilder();
      for (int index = 0; index < inputs.size(); index++) {
        ffmpeg.addInput(UrlInput.fromPath(inputs.get(index)).addArguments("-protocol_whitelist", "file,pipe"));
        filters.append("[").append(index).append(":a:0]aresample=48000,");
        if (monoInputs.get(index)) {
          filters.append("pan=stereo|c0=c0|c1=c0,");
        }
        filters.append("aformat=sample_fmts=fltp:channel_layouts=stereo,asetpts=PTS-STARTPTS[a")
            .append(index).append("];");
        labels.append("[a").append(index).append("]");
      }
      filters.append(labels).append("concat=n=").append(count).append(":v=0:a=1,volume=")
          .append(request.getVolumePercent() / 100.0);
      if (request.getFadeInMs() > 0) {
        filters.append(",afade=t=in:st=0:d=").append(request.getFadeInMs() / 1000.0);
      }
      if (request.getFadeOutMs() > 0) {
        filters.append(",afade=t=out:st=").append((durationMs - request.getFadeOutMs()) / 1000.0)
            .append(":d=").append(request.getFadeOutMs() / 1000.0);
      }
      filters.append("[mixed]");
      var destination = UrlOutput.toPath(output).addArguments("-map", "[mixed]")
          .setCodec(StreamType.AUDIO, format.getFfmpegAudioCodec())
          .addArguments("-ar", "48000").addArguments("-ac", "2").addArguments("-threads", "1");
      if (format.isWavMuxer()) {
        destination.setFormat("wav");
      }
      awaitFfmpeg(ffmpeg.addArguments("-filter_complex_threads", "1").addArguments("-filter_complex", filters.toString()).addOutput(destination)
          .setOverwriteOutput(true).setLogLevel(LogLevel.ERROR).setProgressListener(progress -> {})
          .executeAsync());
      var result = new AudioFileOutputDto(output, format.getMediaType(),
          (join ? "audios-unidos" : stemWithoutExtension(request.getFiles().getFirst().getOriginalFilename()) + "-ajustado")
              + "." + format.getFileExtension());
      delivered = true;
      return result;
    } catch (AudioProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AudioProcessingException("AUDIO_PROCESSING_FAILED", "Não foi possível preparar o áudio.", exception);
    } finally {
      inputs.forEach(AudioService::deleteQuietly);
      if (!delivered) {
        deleteQuietly(output);
      }
    }
  }

  private AudioFileOutputDto convertAudio(AudioConvertRequest request, boolean requireVideo) {
    AudioOutputFormat format = request.getTargetFormat();
    Path inputTemp = null;
    Path outputTemp = null;
    boolean outputDelivered = false;
    try {
      String extensionSuffix = extensionSuffixFromOriginalName(request.getFile().getOriginalFilename());
      inputTemp = Files.createTempFile("helperhub-audio-in-", extensionSuffix);
      outputTemp = Files.createTempFile("helperhub-audio-out-", "." + format.getFileExtension());
      request.getFile().transferTo(inputTemp);

      FFprobeResult probe = runFfprobe(inputTemp);
      if (!containsAudioStream(probe)) {
        throw new AudioProcessingException("NO_AUDIO_STREAM", "O arquivo não contém faixa de áudio detectável.");
      }
      if (requireVideo && probe.getStreams().stream().noneMatch(stream -> StreamType.VIDEO.equals(stream.getCodecType()))) {
        throw new AudioProcessingException("NO_VIDEO_STREAM", "Selecione um vídeo com faixa de áudio para extrair.");
      }

      runFfmpegConvert(inputTemp, outputTemp, format);

      String downloadFilename = requireVideo
          ? stemWithoutExtension(request.getFile().getOriginalFilename()) + "-audio." + format.getFileExtension()
          : buildConvertFilename(request.getFile().getOriginalFilename(), format);
      AudioFileOutputDto result = new AudioFileOutputDto(outputTemp, format.getMediaType(), downloadFilename);
      outputDelivered = true;
      return result;
    } catch (AudioProcessingException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Failed to convert audio", exception);
      throw new AudioProcessingException("AUDIO_PROCESSING_FAILED", "Não foi possível converter o áudio.", exception);
    } finally {
      deleteQuietly(inputTemp);
      if (!outputDelivered) {
        deleteQuietly(outputTemp);
      }
    }
  }

  private void validateCutSegmentLength(long segmentMillis) {
    long maxMs = audioUsageProperties.getMaxCutDurationMs();
    if (segmentMillis > maxMs) {
      throw new AudioProcessingException(
          "CUT_DURATION_LIMIT_EXCEEDED",
          String.format(
              "O trecho solicitado (%d ms) excede o limite máximo permitido (%d ms).",
              segmentMillis,
              maxMs));
    }
  }

  private static void validateCutInterval(long startMs, long endMs, long mediaDurationMs) {
    if (startMs >= mediaDurationMs) {
      throw new AudioProcessingException(
          "START_TIME_OUT_OF_RANGE",
          String.format(
              "O tempo inicial (startMs=%d ms) deve ser menor que a duração do áudio (%d ms).",
              startMs,
              mediaDurationMs));
    }
    if (endMs > mediaDurationMs) {
      throw new AudioProcessingException(
          "END_TIME_OUT_OF_RANGE",
          String.format(
              "O tempo final (endMs=%d ms) não pode ser maior que a duração do áudio (%d ms).",
              endMs,
              mediaDurationMs));
    }
  }

  private long probeAudioDurationMillis(Path inputPath) {
    return audioDurationMillis(runFfprobe(inputPath));
  }

  private static long audioDurationMillis(FFprobeResult result) {
    if (!containsAudioStream(result)) {
      throw new AudioProcessingException(
          "NO_AUDIO_STREAM",
          "O arquivo não contém faixa de áudio detectável.");
    }
    Format format = result.getFormat();
    Float durationSeconds = result.getStreams().stream()
        .filter(stream -> StreamType.AUDIO.equals(stream.getCodecType()))
        .findFirst().map(com.github.kokorin.jaffree.ffprobe.Stream::getDuration).orElse(null);
    if (durationSeconds == null && format != null) {
      durationSeconds = format.getDuration();
    }
    if (Objects.isNull(durationSeconds) || !Float.isFinite(durationSeconds) || durationSeconds <= 0f) {
      throw new AudioProcessingException(
          "DURATION_UNKNOWN",
          "Não foi possível determinar a duração deste arquivo.");
    }
    return Math.round(durationSeconds * 1000f);
  }

  private FFprobeResult runFfprobe(Path inputPath) {
    try {
      var task = FFprobe.atPath()
          .setShowFormat(true)
          .setShowStreams(true)
          .setLogLevel(LogLevel.ERROR)
          .setInput(inputPath)
          .addArguments("-protocol_whitelist", "file,pipe")
          .executeAsync();
      try {
        return task.get(30, java.util.concurrent.TimeUnit.SECONDS);
      } catch (Exception exception) {
        task.cancel(true);
        if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
        throw new AudioProcessingException("PROBE_FAILED", "Não foi possível analisar o áudio dentro do tempo limite.", exception);
      }
    } catch (RuntimeException exception) {
      throw new AudioProcessingException(
          "PROBE_FAILED",
          "Não foi possível analisar o arquivo. Verifique se o FFprobe está instalado junto com o FFmpeg.",
          exception);
    }
  }

  private static boolean containsAudioStream(FFprobeResult result) {
    List<com.github.kokorin.jaffree.ffprobe.Stream> streams = result.getStreams();
    if (Objects.isNull(streams) || streams.isEmpty()) {
      return false;
    }
    for (com.github.kokorin.jaffree.ffprobe.Stream stream : streams) {
      if (StreamType.AUDIO.equals(stream.getCodecType())) {
        return true;
      }
    }
    return false;
  }

  private void runFfmpegCut(Path inputPath, Path outputPath, long startMillis, long durationMillis) {
    try {
      var task = FFmpeg.atPath()
          .addInput(UrlInput.fromPath(inputPath).addArguments("-protocol_whitelist", "file,pipe")
              .setPosition(startMillis)
              .setDuration(durationMillis))
          .addOutput(UrlOutput.toPath(outputPath)
              .addMap(0, "a:0")
              .copyCodec(StreamType.AUDIO)
              .disableStream(StreamType.VIDEO))
          .setOverwriteOutput(true)
          .setLogLevel(LogLevel.ERROR)
          .setProgressListener(progress -> {
          })
          .executeAsync();
      awaitFfmpeg(task);
    } catch (RuntimeException exception) {
      throw new AudioProcessingException(
          "FFMPEG_FAILED",
          "O FFmpeg não conseguiu cortar este arquivo. Verifique se o FFmpeg está instalado e acessível no PATH.",
          exception);
    }
  }

  private void runFfmpegConvert(Path inputPath, Path outputPath, AudioOutputFormat format) {
    try {
      var task = FFmpeg.atPath()
          .addInput(UrlInput.fromPath(inputPath).addArguments("-protocol_whitelist", "file,pipe"))
          .addOutput(buildConvertOutput(outputPath, format))
          .setOverwriteOutput(true)
          .setLogLevel(LogLevel.ERROR)
          .setProgressListener(progress -> {
          })
          .executeAsync();
      awaitFfmpeg(task);
    } catch (RuntimeException exception) {
      throw new AudioProcessingException(
          "FFMPEG_FAILED",
          "O FFmpeg não conseguiu converter este arquivo. Verifique se o FFmpeg está instalado e se o formato de saída é compatível.",
          exception);
    }
  }

  private void awaitFfmpeg(com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture task) {
    try {
      task.get(audioUsageProperties.getMaxProcessingSeconds(), java.util.concurrent.TimeUnit.SECONDS);
    } catch (Exception exception) {
      task.forceStop();
      if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new AudioProcessingException("FFMPEG_FAILED", "Falha ou tempo limite excedido ao processar áudio.", exception);
    }
  }

  private static UrlOutput buildConvertOutput(Path outputPath, AudioOutputFormat format) {
    UrlOutput output = UrlOutput.toPath(outputPath)
        .addMap(0, "a:0")
        .disableStream(StreamType.VIDEO);
    if (format.isWavMuxer()) {
      output.setFormat("wav");
    }
    output.setCodec(StreamType.AUDIO, format.getFfmpegAudioCodec());
    return output;
  }

  private static String extensionSuffixFromOriginalName(String originalFilename) {
    String extension = fileExtension(originalFilename);
    if (extension.isEmpty()) {
      return ".bin";
    }
    return "." + extension;
  }

  private static String fileExtension(String originalFilename) {
    String base = baseFilename(originalFilename);
    int dot = base.lastIndexOf('.');
    if (dot <= 0 || dot == base.length() - 1) {
      return "";
    }
    return base.substring(dot + 1).toLowerCase();
  }

  private static String baseFilename(String originalFilename) {
    if (Objects.isNull(originalFilename) || originalFilename.isBlank()) {
      return "";
    }
    String normalized = originalFilename.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
    return name.isBlank() ? "" : name;
  }

  private static String buildCutFilename(String originalFilename) {
    String base = baseFilename(originalFilename);
    if (base.isEmpty()) {
      return "audio-cut.bin";
    }
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      return base.substring(0, dot) + "-cut" + base.substring(dot);
    }
    return base + "-cut";
  }

  private static String buildConvertFilename(String originalFilename, AudioOutputFormat format) {
    String stem = stemWithoutExtension(originalFilename);
    if (stem.isEmpty()) {
      stem = "audio";
    }
    return stem + "-converted." + format.getFileExtension();
  }

  private static String stemWithoutExtension(String originalFilename) {
    String base = baseFilename(originalFilename);
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      return base.substring(0, dot);
    }
    return base;
  }

  private static String resolveContentType(MultipartFile file) {
    String declared = file.getContentType();
    if (Objects.nonNull(declared) && !declared.isBlank()) {
      return declared;
    }
    return MediaType.APPLICATION_OCTET_STREAM_VALUE;
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
