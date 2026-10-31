package io.helperhub.api.video.service;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.video.config.VideoUsageProperties;
import io.helperhub.api.video.dto.VideoYoutubeDownloadRequest;
import io.helperhub.api.video.exception.VideoProcessingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VideoServiceTest {
  @ParameterizedTest
  @ValueSource(strings = {"https://evil.test/youtube.com/watch?v=abcdefghijk", "https://youtube.com.evil.test/watch?v=abcdefghijk",
      "https://youtube.com@evil.test/watch?v=abcdefghijk", "http://youtube.com/watch?v=abcdefghijk",
      "https://youtube.com:444/watch?v=abcdefghijk", "file:///youtube.com/watch?v=abcdefghijk",
      "https://youtu.be/", "https://youtube.com/watch?v=short", "--help", ""})
  void rejectsUntrustedUrls(String url) {
    assertEquals("INVALID_YOUTUBE_URL", assertThrows(VideoProcessingException.class,
        () -> VideoService.normalizeYoutubeUrl(url)).getError());
  }

  @ParameterizedTest
  @ValueSource(strings = {"https://youtu.be/abcdefghijk?t=10", "https://m.youtube.com/shorts/abcdefghijk",
      " https://www.youtube.com/watch?v=abcdefghijk&list=unwanted "})
  void canonicalizesUrlsAndDropsExtraParameters(String url) {
    assertEquals("https://www.youtube.com/watch?v=abcdefghijk", VideoService.normalizeYoutubeUrl(url));
  }

  @Test
  void timeoutTerminatesRealSilentProcessAndDeletesPartialFiles() {
    VideoUsageProperties props = new VideoUsageProperties();
    props.setMaxDownloadDurationSeconds(1);
    FakeDownloader service = new FakeDownloader(props, "sleep");
    assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
      assertEquals("YOUTUBE_DOWNLOAD_TIMEOUT", assertThrows(VideoProcessingException.class,
          () -> service.downloadFromYoutube(request())).getError());
    });
    assertFalse(Files.exists(service.work));
    assertFalse(ProcessHandle.of(service.pid).map(ProcessHandle::isAlive).orElse(false));
  }

  @Test
  void processFailureRemovesPartialFiles() {
    FakeDownloader service = new FakeDownloader(new VideoUsageProperties(), "fail");
    assertEquals("YOUTUBE_DOWNLOAD_FAILED", assertThrows(VideoProcessingException.class,
        () -> service.downloadFromYoutube(request())).getError());
    assertFalse(Files.exists(service.work));
  }

  @Test
  void successfulDownloadKeepsOnlyTheDeliveredFile() throws Exception {
    FakeDownloader service = new FakeDownloader(new VideoUsageProperties(), "success");
    var output = service.downloadFromYoutube(request());
    try {
      assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(output.tempOutputFile()));
      assertFalse(Files.exists(service.work));
    } finally { Files.deleteIfExists(output.tempOutputFile()); }
  }

  @Test
  void rejectsOversizedCompletedFile() {
    VideoUsageProperties props = new VideoUsageProperties();
    props.setMaxDownloadBytes(2);
    FakeDownloader service = new FakeDownloader(props, "success");
    assertEquals("YOUTUBE_SIZE_LIMIT", assertThrows(VideoProcessingException.class,
        () -> service.downloadFromYoutube(request())).getError());
    assertFalse(Files.exists(service.work));
  }

  private static VideoYoutubeDownloadRequest request() {
    var request = new VideoYoutubeDownloadRequest();
    request.setUrl("https://youtu.be/abcdefghijk");
    return request;
  }

  private static class FakeDownloader extends VideoService {
    Path work;
    long pid;
    final String mode;
    FakeDownloader(VideoUsageProperties props, String mode) { super(props); this.mode = mode; }
    @Override void runCommand(List<String> original, Path directory) {
      assertTrue(original.contains("--ignore-config"));
      assertEquals("https://www.youtube.com/watch?v=abcdefghijk", original.getLast());
      work = directory;
      String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
      String classes;
      try { classes = Path.of(ProcessFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(); }
      catch (Exception exception) { throw new RuntimeException(exception); }
      try { super.runCommand(List.of(java, "-cp", classes, ProcessFixture.class.getName(), mode, directory.toString()), directory); }
      finally {
        try { pid = Long.parseLong(Files.readString(directory.resolve("pid"))); }
        catch (Exception exception) { throw new RuntimeException(exception); }
      }
    }
  }

  public static class ProcessFixture {
    public static void main(String[] args) throws Exception {
      Path work = Path.of(args[1]);
      Files.writeString(work.resolve("pid"), Long.toString(ProcessHandle.current().pid()));
      Files.write(work.resolve("video.part"), new byte[]{1});
      switch (args[0]) {
        case "sleep" -> Thread.sleep(30_000);
        case "fail" -> System.exit(2);
        default -> Files.write(work.resolve("video.mp4"), new byte[]{1, 2, 3});
      }
    }
  }
}
