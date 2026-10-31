package io.helperhub.api.audio.dto;

import java.nio.file.Path;

/**
 * Resultado de processamento: arquivo temporário a ser enviado em streaming (e apagado após o envio).
 */
public record AudioFileOutputDto(Path tempOutputFile, String contentType, String downloadFilename) {

}
