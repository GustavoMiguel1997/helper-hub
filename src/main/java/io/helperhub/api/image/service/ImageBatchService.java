package io.helperhub.api.image.service;

import io.helperhub.api.image.dto.ImageBatchRequest;
import io.helperhub.api.image.dto.ImageCompressRequest;
import io.helperhub.api.image.dto.ImageConvertRequest;
import io.helperhub.api.image.dto.ImageFileOutputDto;
import io.helperhub.api.image.dto.ImageResizeRequest;
import io.helperhub.api.image.exception.ImageProcessingException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageBatchService {
  private static final long MAX_INPUT_BYTES = 50L * 1024 * 1024;
  private static final long MAX_OUTPUT_BYTES = 64L * 1024 * 1024;

  private final ImageResizeService images;
  private final Validator validator;

  public ImageBatchService(ImageResizeService images, Validator validator) {
    this.images = images;
    this.validator = validator;
  }

  public ImageFileOutputDto process(ImageBatchRequest batch) {
    if (batch.getFiles().stream().mapToLong(MultipartFile::getSize).sum() > MAX_INPUT_BYTES) {
      throw new ImageProcessingException("IMAGE_BATCH_INPUT_LIMIT", "O lote deve ter até 50 MB no total.");
    }
    Path archive = null;
    boolean ready = false;
    try {
      archive = Files.createTempFile("helperhub-img-batch-", ".zip");
      try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
        zip.setLevel(Deflater.NO_COMPRESSION);
        long totalBytes = 0;
        int index = 0;
        for (MultipartFile file : batch.getFiles()) {
          index++;
          ImageFileOutputDto result;
          try {
            result = processImage(batch, file);
          } catch (ImageProcessingException exception) {
            throw new ImageProcessingException(exception.getError(),
                "Imagem " + index + " (" + file.getOriginalFilename() + "): " + exception.getMessage(), exception);
          }
          try {
            totalBytes += Files.size(result.tempOutputFile());
            if (totalBytes > MAX_OUTPUT_BYTES) {
              throw new ImageProcessingException("IMAGE_BATCH_OUTPUT_LIMIT",
                  "O resultado excede 64 MB. Reduza as dimensões ou processe menos imagens por lote.");
            }
            String name = result.downloadFilename().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
            zip.putNextEntry(new ZipEntry(index + "-" + name));
            Files.copy(result.tempOutputFile(), zip);
            zip.closeEntry();
          } finally {
            ImageResizeService.deleteQuietly(result.tempOutputFile());
          }
        }
      }
      ready = true;
      return new ImageFileOutputDto(archive, "application/zip", "helperhub-imagens.zip");
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    } finally {
      if (!ready) {
        ImageResizeService.deleteQuietly(archive);
      }
    }
  }

  private ImageFileOutputDto processImage(ImageBatchRequest batch, MultipartFile file) {
    if (batch.getOperation() == ImageBatchRequest.Operation.RESIZE) {
      var request = new ImageResizeRequest();
      request.setFile(file);
      if (batch.getOutputFormat() != null) {
        request.setOutputFormat(batch.getOutputFormat());
      }
      request.setJpegQuality(batch.getJpegQuality());
      request.setMaxWidth(batch.getMaxWidth());
      request.setMaxHeight(batch.getMaxHeight());
      validate(request);
      return images.resize(request);
    }
    if (batch.getOperation() == ImageBatchRequest.Operation.COMPRESS) {
      var request = new ImageCompressRequest();
      request.setFile(file);
      if (batch.getOutputFormat() != null) {
        request.setOutputFormat(batch.getOutputFormat());
      }
      if (batch.getJpegQuality() != null) {
        request.setJpegQuality(batch.getJpegQuality());
      }
      request.setTargetBytes(batch.getTargetBytes());
      request.setAllowResize(batch.isAllowResize());
      validate(request);
      return images.compress(request);
    }
    var request = new ImageConvertRequest();
    request.setFile(file);
    request.setOutputFormat(batch.getOutputFormat());
    request.setJpegQuality(batch.getJpegQuality());
    validate(request);
    return images.convert(request);
  }

  private void validate(Object request) {
    var violations = validator.validate(request);
    if (!violations.isEmpty()) {
      throw new ImageProcessingException("IMAGE_BATCH_INVALID", violations.iterator().next().getMessage());
    }
  }
}
