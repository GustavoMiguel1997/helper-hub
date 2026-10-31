package io.helperhub.api.audio.domain;

import java.util.Objects;

public enum AudioOutputFormat {
  MP3("mp3", "audio/mpeg", "libmp3lame", false),
  WAV("wav", "audio/wav", "pcm_s16le", true),
  AAC("m4a", "audio/mp4", "aac", false),
  OGG("ogg", "audio/ogg", "libvorbis", false),
  FLAC("flac", "audio/flac", "flac", false),
  OPUS("opus", "audio/opus", "libopus", false);

  private final String fileExtension;
  private final String mediaType;
  private final String ffmpegAudioCodec;
  private final boolean wavMuxer;

  AudioOutputFormat(String fileExtension, String mediaType, String ffmpegAudioCodec, boolean wavMuxer) {
    this.fileExtension = fileExtension;
    this.mediaType = mediaType;
    this.ffmpegAudioCodec = ffmpegAudioCodec;
    this.wavMuxer = wavMuxer;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public String getMediaType() {
    return mediaType;
  }

  public String getFfmpegAudioCodec() {
    return ffmpegAudioCodec;
  }

  public boolean isWavMuxer() {
    return wavMuxer;
  }

  public static AudioOutputFormat fromString(String raw) {
    if (Objects.isNull(raw) || raw.isBlank()) {
      return null;
    }
    return AudioOutputFormat.valueOf(raw.trim().toUpperCase());
  }
}
