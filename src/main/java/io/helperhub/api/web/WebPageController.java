package io.helperhub.api.web;

import io.helperhub.api.image.config.ImageUsageProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega as páginas HTML SSR das ferramentas e o sitemap XML dinâmico do HelperHub.
 */
@RestController
public class WebPageController {

  private final ToolPageRenderer renderer;
  private final ImageUsageProperties imageUsageProperties;

  public WebPageController(
      ToolPageRenderer renderer,
      ImageUsageProperties imageUsageProperties) {
    this.renderer = renderer;
    this.imageUsageProperties = imageUsageProperties;
  }

  @GetMapping(
      value = {
        "/",
        "/comprimir-imagem",
        "/comprimir-imagem-500kb",
        "/converter-png-para-jpg",
        "/redimensionar-imagem",
        "/recortar-imagem",
        "/girar-espelhar-imagem",
        "/converter-imagem",
        "/remover-fundo-imagem",
        "/cortar-audio",
        "/converter-audio",
        "/juntar-audios",
        "/ajustar-volume-audio",
        "/extrair-audio-video",
        "/baixar-video-youtube"
      },
      produces = "text/html;charset=UTF-8")
  public ResponseEntity<String> renderToolPage(HttpServletRequest request) {
    String contextPath = Objects.requireNonNullElse(request.getContextPath(), "");
    String path = request.getRequestURI().substring(contextPath.length());
    ToolPageDefinition page = ToolPageCatalog.findByPath(path)
        .orElseGet(ToolPageCatalog::home);
    String html = renderer.renderPage(page, isRemoveBgEnabled());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
        .body(html);
  }

  @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
  public ResponseEntity<String> sitemap() {
    String xml = renderer.renderSitemapXml(isRemoveBgEnabled());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
        .body(xml);
  }

  private boolean isRemoveBgEnabled() {
    String apiKey = imageUsageProperties.getRemoveBgApiKey();
    return Objects.nonNull(apiKey) && !apiKey.isBlank();
  }
}
