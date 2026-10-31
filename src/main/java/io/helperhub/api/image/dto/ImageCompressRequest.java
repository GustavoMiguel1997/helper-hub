package io.helperhub.api.image.dto;

import io.helperhub.api.image.domain.ImageResizeOutputFormat;

/** Compression defaults; inherited upload, format and quality validation still applies. */
public class ImageCompressRequest extends ImageConvertRequest {
  public ImageCompressRequest() {
    setOutputFormat(ImageResizeOutputFormat.JPEG);
    setJpegQuality(75);
  }
}
