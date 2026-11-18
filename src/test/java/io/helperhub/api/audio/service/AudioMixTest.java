package io.helperhub.api.audio.service;

import static org.junit.jupiter.api.Assertions.*;

import io.helperhub.api.audio.config.AudioUsageProperties;
import io.helperhub.api.audio.domain.AudioOutputFormat;
import io.helperhub.api.audio.dto.AudioFileOutputDto;
import io.helperhub.api.audio.dto.AudioMixRequest;
import io.helperhub.api.audio.exception.AudioProcessingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AudioMixTest {
  private final AudioService service = new AudioService(new AudioUsageProperties());

  private static MockMultipartFile tone(int frequency, int rate) throws Exception {
    byte[] pcm = new byte[rate*2];
    for (int frame = 0; frame < rate; frame++) {
      short sample = (short) (Math.sin(2*Math.PI*frequency*frame/rate)*6000);
      pcm[frame*2] = (byte) sample;
      pcm[frame*2+1] = (byte) (sample >> 8);
    }
    var output = new ByteArrayOutputStream();
    try (var stream = new AudioInputStream(new ByteArrayInputStream(pcm),
        new AudioFormat(rate,16,1,true,false),rate)) {
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output);
    }
    return new MockMultipartFile("files","same-name.wav","audio/wav",output.toByteArray());
  }

  private static double[] samples(AudioFileOutputDto result) throws Exception {
    try (var stream = AudioSystem.getAudioInputStream(result.tempOutputFile().toFile())) {
      assertEquals(48000, stream.getFormat().getSampleRate());
      assertEquals(2, stream.getFormat().getChannels());
      byte[] bytes = stream.readAllBytes();
      double[] samples = new double[bytes.length/4];
      for (int index = 0; index < samples.length; index++) {
        samples[index] = (short) ((bytes[index*4] & 255) | (bytes[index*4+1] << 8));
      }
      return samples;
    } finally {
      Files.deleteIfExists(result.tempOutputFile());
    }
  }

  private static int crossings(double[] samples, int start, int end) {
    int crossings = 0;
    for (int index = start+1; index < end; index++) {
      if (samples[index] > 0 && samples[index-1] <= 0) {
        crossings++;
      }
    }
    return crossings;
  }

  private static double rms(double[] samples, int start, int end) {
    double sum = 0;
    for (int index = start; index < end; index++) {
      sum += samples[index]*samples[index];
    }
    return Math.sqrt(sum/(end-start));
  }

  @Test
  void joinsDifferentSampleRatesInSubmittedOrder() throws Exception {
    var request = new AudioMixRequest();
    request.setFiles(List.of(tone(800,16000),tone(400,8000)));
    request.setTargetFormat(AudioOutputFormat.WAV);
    var output = samples(service.mixAudio(request,true));
    assertEquals(96000,output.length,100);
    assertEquals(400,crossings(output,12000,36000),2);
    assertEquals(200,crossings(output,60000,84000),2);
  }

  @Test
  void volumeAndFadesChangeTheActualSamples() throws Exception {
    var request = new AudioMixRequest();
    request.setFiles(List.of(tone(400,16000)));
    request.setTargetFormat(AudioOutputFormat.WAV);
    var original = samples(service.mixAudio(request,false));
    assertEquals(6000/Math.sqrt(2),rms(original,18000,30000),50);
    request.setVolumePercent(50);
    request.setFadeInMs(250);
    request.setFadeOutMs(250);
    var adjusted = samples(service.mixAudio(request,false));
    assertEquals(0.5,rms(adjusted,18000,30000)/rms(original,18000,30000),0.02);
    assertTrue(rms(adjusted,0,2400) < rms(adjusted,18000,30000)*0.2);
    assertTrue(rms(adjusted,45600,48000) < rms(adjusted,18000,30000)*0.2);
  }

  @Test
  void rejectsFadePastEndAndExcessiveDuration() throws Exception {
    var request = new AudioMixRequest();
    request.setFiles(List.of(tone(400,8000)));
    request.setFadeInMs(2000);
    assertEquals("AUDIO_FADE_DURATION",
        assertThrows(AudioProcessingException.class,()->service.mixAudio(request,false)).getError());
    request.setFadeInMs(0);
    var limits = new AudioUsageProperties();
    limits.setMaxEditDurationMs(500);
    assertEquals("AUDIO_DURATION_LIMIT",
        assertThrows(AudioProcessingException.class,()->new AudioService(limits).mixAudio(request,false)).getError());
  }
}
