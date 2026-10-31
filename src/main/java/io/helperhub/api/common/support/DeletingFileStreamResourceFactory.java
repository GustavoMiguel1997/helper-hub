package io.helperhub.api.common.support;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public final class DeletingFileStreamResourceFactory {

  private static final Logger log = LoggerFactory.getLogger(DeletingFileStreamResourceFactory.class);

  private DeletingFileStreamResourceFactory() {
  }

  /**
   * Cria um {@link Resource} que lê o ficheiro e apaga-o ao fechar o stream (após o envio HTTP).
   */
  public static Resource createDeletingOnClose(Path tempFile) {
    Objects.requireNonNull(tempFile, "tempFile");
    try {
      InputStream delegate = Files.newInputStream(tempFile);
      FilterInputStream deletingStream = new FilterInputStream(delegate) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } finally {
            deleteQuietly(tempFile);
          }
        }
      };
      long length = Files.size(tempFile);
      return new InputStreamResource(deletingStream) {
        @Override
        public long contentLength() {
          return length;
        }
      };
    } catch (IOException exception) {
      deleteQuietly(tempFile);
      throw new UncheckedIOException("Não foi possível preparar o arquivo para download.", exception);
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      log.warn("Não foi possível apagar arquivo temporário {}", path, exception);
    }
  }
}
