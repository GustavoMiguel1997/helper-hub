package io.helperhub.api.image.domain;

public enum ImageResizeOutputFormat {
  AUTO(null, null, null),
  JPEG("jpg", "image/jpeg", "jpg"),
  PNG("png", "image/png", "png");

  private final String fileExtension;
  private final String mediaType;
  private final String thumbnailFormat;

  ImageResizeOutputFormat(String fileExtension, String mediaType, String thumbnailFormat) {
    this.fileExtension = fileExtension;
    this.mediaType = mediaType;
    this.thumbnailFormat = thumbnailFormat;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public String getMediaType() {
    return mediaType;
  }

  public String getThumbnailFormat() {
    return thumbnailFormat;
  }

  public boolean isAuto() {
    return this == AUTO;
  }
}
