package io.helperhub.api.audio.service;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.audio.config.AudioUsageProperties;
import io.helperhub.api.audio.domain.AudioOutputFormat;
import io.helperhub.api.audio.dto.*;
import java.io.*;
import java.nio.file.Files;
import javax.sound.sampled.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

@Tag("media")
class AudioProcessingTest {
  @Test void convertsRealWavToMp3() throws Exception {
    var request = new AudioConvertRequest(); request.setFile(wav()); request.setTargetFormat(AudioOutputFormat.MP3);
    var result = new AudioService(new AudioUsageProperties()).convertAudio(request);
    try {
      assertTrue(Files.size(result.tempOutputFile()) > 100);
      var probe = com.github.kokorin.jaffree.ffprobe.FFprobe.atPath().setShowStreams(true)
          .setInput(result.tempOutputFile()).execute();
      assertEquals("mp3", probe.getStreams().getFirst().getCodecName());
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }

  @Test void cutsRealWavToRequestedDuration() throws Exception {
    var request = new AudioCutRequest(); request.setFile(wav()); request.setStartMs(250L); request.setEndMs(750L);
    var result = new AudioService(new AudioUsageProperties()).cutAudio(request);
    try (var audio = AudioSystem.getAudioInputStream(result.tempOutputFile().toFile())) {
      double duration = audio.getFrameLength() / audio.getFormat().getFrameRate();
      assertEquals(0.5, duration, 0.1);
    } finally { Files.deleteIfExists(result.tempOutputFile()); }
  }

  private static MockMultipartFile wav() throws Exception {
    var format = new AudioFormat(44100, 16, 1, true, false);
    byte[] pcm = new byte[44100 * 2];
    for (int i = 0; i < 44100; i++) {
      short sample = (short) (Math.sin(2 * Math.PI * 440 * i / 44100) * 5000);
      pcm[2 * i] = (byte) sample; pcm[2 * i + 1] = (byte) (sample >> 8);
    }
    var out = new ByteArrayOutputStream();
    try (var stream = new AudioInputStream(new ByteArrayInputStream(pcm), format, 44100)) {
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out);
    }
    return new MockMultipartFile("file", "tone.wav", "audio/wav", out.toByteArray());
  }
}
