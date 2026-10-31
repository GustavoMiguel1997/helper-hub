package io.helperhub.api.audio.service;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.audio.config.AudioUsageProperties;
import io.helperhub.api.audio.domain.AudioOutputFormat;
import io.helperhub.api.audio.dto.AudioConvertRequest;
import io.helperhub.api.audio.exception.AudioProcessingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AudioExtractionTest {
  @TempDir Path work;

  private AudioConvertRequest video(boolean withAudio) throws Exception {
    Path input = work.resolve("input.mp4");
    var command = new ArrayList<>(List.of("ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
        "-f", "lavfi", "-i", "color=c=blue:s=64x64:r=10"));
    if (withAudio) command.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100"));
    command.addAll(List.of("-t", "1", "-c:v", "mpeg4", "-c:a", "aac", input.toString()));
    Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
    try { assertTrue(process.waitFor(15, TimeUnit.SECONDS)); assertEquals(0, process.exitValue()); }
    finally { if (process.isAlive()) process.destroyForcibly(); }
    var request = new AudioConvertRequest(); request.setTargetFormat(AudioOutputFormat.MP3);
    request.setFile(new MockMultipartFile("file", "clip.mp4", "video/mp4", Files.readAllBytes(input)));
    return request;
  }

  @Test void extractsOnlyAudioFromRealVideo() throws Exception {
    var result = new AudioService(new AudioUsageProperties()).extractAudio(video(true));
    try {
      var probe = com.github.kokorin.jaffree.ffprobe.FFprobe.atPath().setShowStreams(true).setShowFormat(true)
          .setInput(result.tempOutputFile()).execute();
      assertEquals(1, probe.getStreams().size());
      assertEquals("mp3", probe.getStreams().getFirst().getCodecName());
      assertEquals(com.github.kokorin.jaffree.StreamType.AUDIO, probe.getStreams().getFirst().getCodecType());
      assertEquals(1.0, probe.getFormat().getDuration(), 0.15);
      assertEquals("clip-audio.mp3", result.downloadFilename());
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }

  @Test void refusesSilentVideo() throws Exception {
    var request = video(false);
    assertEquals("NO_AUDIO_STREAM", assertThrows(AudioProcessingException.class,
        () -> new AudioService(new AudioUsageProperties()).extractAudio(request)).getError());
  }

  @Test void refusesAnAudioOnlyFile() throws Exception {
    var service = new AudioService(new AudioUsageProperties());
    var result = service.extractAudio(video(true));
    try {
      var request = new AudioConvertRequest(); request.setTargetFormat(AudioOutputFormat.WAV);
      request.setFile(new MockMultipartFile("file", "audio.mp3", "audio/mpeg", Files.readAllBytes(result.tempOutputFile())));
      assertEquals("NO_VIDEO_STREAM", assertThrows(AudioProcessingException.class,
          () -> service.extractAudio(request)).getError());
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }
}
