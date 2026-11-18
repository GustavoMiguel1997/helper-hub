package io.helperhub.api.video.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helperhub.api.video.config.VideoUsageProperties;
import io.helperhub.api.video.dto.VideoCompressRequest;
import io.helperhub.api.video.exception.VideoProcessingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

class VideoCompressionTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final VideoCompressionService service = new VideoCompressionService(new VideoUsageProperties());
  @TempDir Path temp;

  private static ObjectNode metadata() throws Exception {
    return (ObjectNode) JSON.readTree("""
        {"format":{"format_name":"mov,mp4,m4a,3gp,3g2,mj2","duration":"5.0"},
         "streams":[{"index":0,"codec_type":"video","width":1920,"height":1080,"avg_frame_rate":"30000/1001"}]}
        """);
  }

  @ParameterizedTest
  @CsvSource({"duration,301,VIDEO_DURATION_LIMIT", "duration,NaN,VIDEO_DURATION_LIMIT",
      "width,8192,VIDEO_DIMENSION_LIMIT", "height,8192,VIDEO_DIMENSION_LIMIT",
      "width,2,VIDEO_DIMENSION_LIMIT", "avg_frame_rate,120/1,VIDEO_FPS_LIMIT",
      "avg_frame_rate,0/0,VIDEO_FPS_LIMIT", "format_name,hls,VIDEO_FORMAT_INVALID",
      "codec_type,audio,VIDEO_AUDIO_LIMIT"})
  void rejectsDangerousOrUnsupportedMetadata(String field, String value, String error) throws Exception {
    ObjectNode root = metadata();
    ObjectNode target = (ObjectNode) (field.equals("duration") || field.equals("format_name")
        ? root.get("format") : root.get("streams").get(0));
    target.put(field,value);
    assertEquals(error,assertThrows(VideoProcessingException.class,() -> service.validateMetadata(root)).getError());
  }

  @Test void rejectsTooManyStreamsAndAudioChannelsAndCoverArt() throws Exception {
    ObjectNode root = metadata();
    var streams = (com.fasterxml.jackson.databind.node.ArrayNode) root.get("streams");
    for (int i=0;i<16;i++) streams.add(streams.get(0).deepCopy());
    assertEquals("VIDEO_STREAM_LIMIT",assertThrows(VideoProcessingException.class,() -> service.validateMetadata(root)).getError());
    ObjectNode cover = metadata();
    ((ObjectNode) cover.get("streams").get(0)).putObject("disposition").put("attached_pic",1);
    assertEquals("NO_VIDEO_STREAM",assertThrows(VideoProcessingException.class,() -> service.validateMetadata(cover)).getError());
    ObjectNode channels = metadata();
    ((com.fasterxml.jackson.databind.node.ArrayNode)channels.get("streams")).addObject().put("codec_type","audio").put("channels",16);
    assertEquals("VIDEO_AUDIO_LIMIT",assertThrows(VideoProcessingException.class,() -> service.validateMetadata(channels)).getError());
  }

  @Test void usesLongestTrackAndAcceptsStreamDurationWhenContainerDurationMissing() throws Exception {
    ObjectNode root = metadata();
    ((ObjectNode)root.get("streams").get(0)).put("duration","301");
    assertEquals("VIDEO_DURATION_LIMIT",assertThrows(VideoProcessingException.class,() -> service.validateMetadata(root)).getError());
    ((ObjectNode)root.get("format")).remove("duration");
    ((ObjectNode)root.get("streams").get(0)).put("duration","4.5");
    assertEquals(4.5,service.validateMetadata(root).duration());
  }

  @ParameterizedTest @ValueSource(strings={"sleep","oversize","fail"})
  void boundsProcessesAndKillsThem(String mode) throws Exception {
    Path output = temp.resolve("out");
    Path pid = temp.resolve("pid");
    String java = Path.of(System.getProperty("java.home"),"bin","java").toString();
    String classes = Path.of(ProcessFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    String expected = mode.equals("sleep") ? "VIDEO_PROCESSING_TIMEOUT" : mode.equals("oversize") ? "VIDEO_OUTPUT_LIMIT" : "VIDEO_PROCESSING_FAILED";
    assertTimeoutPreemptively(Duration.ofSeconds(10),() -> assertEquals(expected,
        assertThrows(VideoProcessingException.class,() -> service.run(
            List.of(java,"-cp",classes,ProcessFixture.class.getName(),mode,output.toString(),pid.toString()),
            null,output,2,1)).getError()));
    assertFalse(ProcessHandle.of(Long.parseLong(Files.readString(pid))).map(ProcessHandle::isAlive).orElse(false));
  }

  @Test void corruptAndPlaylistInputsAreRejectedAndWorkDirectoriesRemoved() throws Exception {
    for (String content : List.of("fake video", "#EXTM3U\n#EXT-X-TARGETDURATION:5\n#EXTINF:5,\nhttp://127.0.0.1:1/private.ts\n#EXT-X-ENDLIST\n")) {
      var request = request(content.getBytes(),"movie.mp4");
      var observed = new VideoCompressionService(new VideoUsageProperties()) {
        Path work;
        @Override void run(List<String> command,Path stdout,Path bounded,long maximum,long seconds) {
          work = bounded.getParent();
          assertTrue(command.contains("file"));
          assertTrue(command.contains("mov,matroska,webm,avi,asf,mpeg,mpegts"));
          super.run(command,stdout,bounded,maximum,seconds);
        }
      };
      assertEquals("VIDEO_PROCESSING_FAILED",assertThrows(VideoProcessingException.class,() -> observed.compress(request)).getError());
      assertNotNull(observed.work);
      assertFalse(Files.exists(observed.work));
    }
  }

  @Test void compressesRealVideoPreservingAudioAndDuration() throws Exception {
    Path input = temp.resolve("source.mp4");
    ffmpeg("-f","lavfi","-i","testsrc2=size=640x360:rate=30:duration=3",
        "-f","lavfi","-i","sine=frequency=440:duration=3","-c:v","libx264","-crf","0",
        "-c:a","aac","-shortest",input.toString());
    var output = service.compress(request(Files.readAllBytes(input),"../vídeo.mp4"));
    try {
      assertTrue(Files.size(output.tempOutputFile()) < Files.size(input));
      assertEquals("vídeo-compressed.mp4",output.downloadFilename());
      var metadata = probe(output.tempOutputFile());
      assertEquals("h264",metadata.get("streams").get(0).get("codec_name").asText());
      assertEquals("aac",metadata.get("streams").get(1).get("codec_name").asText());
      assertEquals(3,metadata.get("format").get("duration").asDouble(),0.15);
    } finally { Files.delete(output.tempOutputFile()); }
  }

  @ParameterizedTest @ValueSource(strings={"361x641","641x361"})
  void handlesSilentPortraitLandscapeAndOddDimensions(String size) throws Exception {
    Path input = temp.resolve("source.mkv");
    ffmpeg("-f","lavfi","-i","testsrc=size="+size+":rate=24:duration=1",
        "-c:v","ffv1",input.toString());
    var request = request(Files.readAllBytes(input),"movie.mkv");
    request.setResolution(480);
    var output = service.compress(request);
    try {
      var streams = probe(output.tempOutputFile()).get("streams");
      assertEquals(1,streams.size());
      int w = streams.get(0).get("width").asInt(), h = streams.get(0).get("height").asInt();
      assertTrue(Math.min(w,h) <= 480 && Math.max(w,h) <= 854);
      assertEquals(0,w%2); assertEquals(0,h%2);
      assertEquals(size.startsWith("361"),h>w);
      assertEquals("30/1",streams.get(0).get("r_frame_rate").asText());
    } finally { Files.delete(output.tempOutputFile()); }
  }

  private static VideoCompressRequest request(byte[] bytes,String name) {
    var request = new VideoCompressRequest();
    request.setFile(new MockMultipartFile("file",name,"video/mp4",bytes));
    return request;
  }

  private void ffmpeg(String... arguments) throws Exception {
    var command = new java.util.ArrayList<>(List.of("ffmpeg","-nostdin","-y","-loglevel","error","-threads","1"));
    command.addAll(List.of(arguments));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(temp.resolve("ffmpeg.log").toFile()).start();
    try {
      assertTrue(process.waitFor(20,TimeUnit.SECONDS));
      assertEquals(0,process.exitValue(),() -> {
        try { return Files.readString(temp.resolve("ffmpeg.log")); } catch (Exception e) { return e.toString(); }
      });
    } finally { VideoService.terminate(process); }
  }

  private com.fasterxml.jackson.databind.JsonNode probe(Path input) throws Exception {
    Path json = temp.resolve("probe.json");
    Process process = new ProcessBuilder("ffprobe","-v","error","-show_streams","-show_format","-of","json",input.toString())
        .redirectOutput(json.toFile()).start();
    try {
      assertTrue(process.waitFor(10,TimeUnit.SECONDS)); assertEquals(0,process.exitValue());
      return JSON.readTree(json.toFile());
    } finally { VideoService.terminate(process); }
  }

  public static class ProcessFixture {
    public static void main(String[] args) throws Exception {
      Files.writeString(Path.of(args[2]),Long.toString(ProcessHandle.current().pid()));
      Files.write(Path.of(args[1]),args[0].equals("oversize") ? new byte[]{1,2,3} : new byte[]{1});
      if (args[0].equals("fail")) System.exit(2);
      Thread.sleep(30_000);
    }
  }
}
