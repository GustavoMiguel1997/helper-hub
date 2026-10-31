package io.helperhub.api.video.dto;

import java.nio.file.Path;

public record VideoFileOutputDto(Path tempOutputFile, String contentType, String downloadFilename) {

}
