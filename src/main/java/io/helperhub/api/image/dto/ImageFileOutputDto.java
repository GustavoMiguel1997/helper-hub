package io.helperhub.api.image.dto;

import java.nio.file.Path;

public record ImageFileOutputDto(Path tempOutputFile, String contentType, String downloadFilename) {

}
