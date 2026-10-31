package io.helperhub.api.video.domain;

public enum VideoOutputFormat {
  MP4("mp4", "video/mp4"),
  WEBM("webm", "video/webm");

  private final String fileExtension;
  private final String mediaType;

  VideoOutputFormat(String fileExtension, String mediaType) {
    this.fileExtension = fileExtension;
    this.mediaType = mediaType;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public String getMediaType() {
    return mediaType;
  }
}
