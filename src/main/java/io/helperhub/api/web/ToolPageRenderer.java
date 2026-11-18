package io.helperhub.api.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helperhub.api.web.ToolPageDefinition.FaqItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Renderiza páginas HTML SSR e o sitemap XML a partir do catálogo de ferramentas.
 */
@Component
public class ToolPageRenderer {

  private static final List<String> CATEGORY_ORDER = List.of("IMAGEM", "ÁUDIO", "VÍDEO");
  private static final Map<String, String> CATEGORY_TITLES = Map.of(
      "IMAGEM", "Imagem",
      "ÁUDIO", "Áudio",
      "VÍDEO", "Vídeo"
  );

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String templateHtml;

  public ToolPageRenderer() {
    this.templateHtml = loadTemplate("templates/helperhub.html");
  }

  public String renderPage(ToolPageDefinition page, boolean removeBgEnabled) {
    String canonicalUrl = ToolPageCatalog.BASE_URL + page.path();
    boolean indexable = page.homePage() || !"removeBg".equals(page.toolKey()) || removeBgEnabled;
    String robots = indexable ? "index, follow" : "noindex, follow";

    return templateHtml
        .replace("{{TITLE}}", escape(page.title()))
        .replace("{{DESCRIPTION}}", escape(page.metaDescription()))
        .replace("{{ROBOTS}}", robots)
        .replace("{{CANONICAL_URL}}", escape(canonicalUrl))
        .replace("{{OG_TITLE}}", escape(page.title()))
        .replace("{{OG_DESCRIPTION}}", escape(page.metaDescription()))
        .replace("{{JSON_LD}}", buildJsonLd(page, canonicalUrl))
        .replace("{{INITIAL_TOOL}}", escape(page.toolKey()))
        .replace("{{INTRO_EYEBROW}}", escape(page.eyebrow()))
        .replace("{{H1}}", escape(page.h1()))
        .replace("{{LEAD}}", escape(page.lead()))
        .replace("{{TOOL_NAV}}", buildToolNav(page))
        .replace("{{BREADCRUMB}}", buildBreadcrumb(page))
        .replace("{{EXPLORE_CONTENT}}", buildExploreContent())
        .replace("{{EXPLORE_HIDDEN}}", page.homePage() ? "" : "hidden")
        .replace("{{EDITOR_HIDDEN}}", page.homePage() ? "hidden" : "")
        .replace("{{SEO_CONTENT}}", buildSeoContent(page, removeBgEnabled))
        .replace("{{PAGE_CATALOG_JSON}}", buildClientCatalogJson(removeBgEnabled));
  }

  public String renderSitemapXml(boolean removeBgEnabled) {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    appendUrlEntry(xml, ToolPageCatalog.BASE_URL + "/", "1.0");
    for (ToolPageDefinition tool : indexableTools(removeBgEnabled)) {
      appendUrlEntry(xml, ToolPageCatalog.BASE_URL + tool.path(), "0.9");
    }
    xml.append("</urlset>\n");
    return xml.toString();
  }

  private static void appendUrlEntry(StringBuilder xml, String loc, String priority) {
    xml.append("  <url>\n")
        .append("    <loc>").append(escape(loc)).append("</loc>\n")
        .append("    <changefreq>monthly</changefreq>\n")
        .append("    <priority>").append(priority).append("</priority>\n")
        .append("  </url>\n");
  }

  private static List<ToolPageDefinition> indexableTools(boolean removeBgEnabled) {
    return ToolPageCatalog.pages().stream()
        .filter(tool -> removeBgEnabled || !"removeBg".equals(tool.toolKey()))
        .toList();
  }

  private static String buildToolNav(ToolPageDefinition currentPage) {
    StringBuilder nav = new StringBuilder();
    nav.append("<a class=\"explore-link\" href=\"/\" data-home")
        .append(currentPage.homePage() ? " aria-current=\"page\"" : "")
        .append(">Explore as ferramentas</a>\n");
    for (String category : CATEGORY_ORDER) {
      boolean open = category.equals(currentPage.category());
      nav.append("<details class=\"tool-category\" data-category=\"")
          .append(escape(category))
          .append("\" name=\"tool-category\"")
          .append(open ? " open" : "")
          .append(">\n");
      long count = ToolPageCatalog.tools().stream().filter(tool -> category.equals(tool.category())).count();
      nav.append("<summary><span>").append(escape(CATEGORY_TITLES.get(category)))
          .append("</span><small>").append(count).append(" ferramentas</small></summary>\n");
      nav.append("<div class=\"tool-links\">\n");
      for (ToolPageDefinition tool : ToolPageCatalog.tools()) {
        if (!category.equals(tool.category())) {
          continue;
        }
        boolean active = !currentPage.homePage() && tool.toolKey().equals(currentPage.toolKey());
        nav.append("<a class=\"tool-link\" href=\"")
            .append(escape(tool.path()))
            .append("\" data-tool=\"")
            .append(escape(tool.toolKey()))
            .append("\" aria-pressed=\"")
            .append(active)
            .append("\"");
        if (!currentPage.homePage() && tool.path().equals(currentPage.path())) {
          nav.append(" aria-current=\"page\"");
        }
        nav.append(">")
            .append(escape(tool.navLabel()))
            .append("</a>\n");
      }
      nav.append("</div></details>\n");
    }
    return nav.toString();
  }

  private static String buildExploreContent() {
    StringBuilder html = new StringBuilder();
    var featuredKeys = List.of("compress", "crop", "cut", "audioJoin", "extract", "youtube");
    for (String category : CATEGORY_ORDER) {
      html.append("<section class=\"explore-category\"><h3>")
          .append(escape(CATEGORY_TITLES.get(category))).append("</h3><div class=\"explore-cards\">");
      for (ToolPageDefinition tool : ToolPageCatalog.tools()) {
        if (category.equals(tool.category()) && featuredKeys.contains(tool.toolKey())) {
          html.append("<a class=\"explore-card\" href=\"").append(escape(tool.path()))
              .append("\" data-tool=\"").append(escape(tool.toolKey())).append("\">")
              .append("<strong>").append(escape(tool.navLabel())).append(" <span aria-hidden=\"true\">↗</span></strong>")
              .append("<span>").append(escape(tool.lead())).append("</span></a>");
        }
      }
      html.append("</div></section>");
    }
    return html.toString();
  }

  private static String buildBreadcrumb(ToolPageDefinition page) {
    if (page.homePage()) {
      return "<span aria-current=\"page\">Explore</span>";
    }
    return "<a href=\"/\">Explore</a><span aria-hidden=\"true\">/</span><span aria-current=\"page\">"
        + escape(page.navLabel()) + "</span>";
  }

  private static String buildSeoContent(ToolPageDefinition page, boolean removeBgEnabled) {
    StringBuilder html = new StringBuilder();
    html.append("<article class=\"seo-overview\">\n");
    html.append("<h2>").append(escape(page.summaryHeading())).append("</h2>\n");
    for (String paragraph : page.summaryParagraphs()) {
      html.append("<p>").append(escape(paragraph)).append("</p>\n");
    }
    html.append("</article>\n");

    if (page.homePage()) {
      html.append("<div class=\"seo-directory\" aria-label=\"Catálogo de ferramentas\">\n");
      html.append("<h3>Ferramentas disponíveis</h3>\n");
      html.append("<div class=\"seo-cards\">\n");
      for (ToolPageDefinition tool : indexableTools(removeBgEnabled)) {
        html.append("<a class=\"seo-card\" href=\"").append(escape(tool.path())).append("\">");
        html.append("<span class=\"seo-card-cat\">").append(escape(tool.category())).append("</span>");
        html.append("<strong>").append(escape(tool.navLabel())).append("</strong>");
        html.append("<span>").append(escape(tool.lead())).append("</span>");
        html.append("</a>\n");
      }
      html.append("</div>\n</div>\n");
    }

    if (!page.faqs().isEmpty()) {
      html.append("<div class=\"seo-faq\">\n");
      html.append("<h3>Perguntas frequentes</h3>\n");
      html.append("<dl>\n");
      for (FaqItem faq : page.faqs()) {
        html.append("<div class=\"seo-faq-item\">");
        html.append("<dt>").append(escape(faq.question())).append("</dt>");
        html.append("<dd>").append(escape(faq.answer())).append("</dd>");
        html.append("</div>\n");
      }
      html.append("</dl>\n</div>\n");
    }

    if (!page.homePage()) {
      html.append("<nav class=\"seo-related\" aria-label=\"Outras ferramentas\">\n");
      html.append("<h3>Outras ferramentas do HelperHub</h3>\n");
      html.append("<ul>\n");
      for (ToolPageDefinition other : indexableTools(removeBgEnabled)) {
        if (other.path().equals(page.path())) {
          continue;
        }
        html.append("<li><a href=\"").append(escape(other.path())).append("\">")
            .append(escape(other.navLabel())).append("</a></li>\n");
      }
      html.append("</ul>\n</nav>\n");
    }
    return html.toString();
  }

  private static String buildJsonLd(ToolPageDefinition page, String canonicalUrl) {
    List<Map<String, Object>> graph = new ArrayList<>();
    Map<String, Object> appNode = new LinkedHashMap<>();
    appNode.put("@type", "WebApplication");
    appNode.put("name", page.homePage() ? "HelperHub" : "HelperHub — " + page.navLabel());
    appNode.put("url", canonicalUrl);
    appNode.put("applicationCategory", "MultimediaApplication");
    appNode.put("operatingSystem", "Web");
    appNode.put("inLanguage", "pt-BR");
    appNode.put("isAccessibleForFree", true);
    appNode.put("description", page.metaDescription());
    graph.add(appNode);

    if (!page.homePage()) {
      graph.add(Map.of(
          "@type", "BreadcrumbList",
          "itemListElement", List.of(
              Map.of("@type", "ListItem", "position", 1, "name", "Explore",
                  "item", ToolPageCatalog.BASE_URL + "/"),
              Map.of("@type", "ListItem", "position", 2, "name", page.navLabel(),
                  "item", canonicalUrl)
          )
      ));
    }

    if (!page.faqs().isEmpty()) {
      List<Map<String, Object>> entities = new ArrayList<>();
      for (FaqItem faq : page.faqs()) {
        entities.add(Map.of(
            "@type", "Question",
            "name", faq.question(),
            "acceptedAnswer", Map.of(
                "@type", "Answer",
                "text", faq.answer()
            )
        ));
      }
      graph.add(Map.of(
          "@type", "FAQPage",
          "mainEntity", entities
      ));
    }

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("@context", "https://schema.org");
    root.put("@graph", graph);
    return toSafeScriptJson(root);
  }

  private String buildClientCatalogJson(boolean removeBgEnabled) {
    List<Map<String, Object>> entries = new ArrayList<>();
    entries.add(toClientEntry(ToolPageCatalog.home(), removeBgEnabled));
    for (ToolPageDefinition tool : ToolPageCatalog.pages()) {
      entries.add(toClientEntry(tool, removeBgEnabled));
    }
    return toSafeScriptJson(entries);
  }

  private static Map<String, Object> toClientEntry(ToolPageDefinition page, boolean removeBgEnabled) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("path", page.path());
    entry.put("toolKey", page.toolKey());
    entry.put("presets", ToolPageCatalog.presetValues(page.path()));
    entry.put("breadcrumbHtml", buildBreadcrumb(page));
    entry.put("title", page.title());
    entry.put("description", page.metaDescription());
    entry.put("robots", page.homePage() || !"removeBg".equals(page.toolKey()) || removeBgEnabled
        ? "index, follow" : "noindex, follow");
    entry.put("jsonLd", buildJsonLd(page, ToolPageCatalog.BASE_URL + page.path()));
    entry.put("canonicalUrl", ToolPageCatalog.BASE_URL + page.path());
    entry.put("eyebrow", page.eyebrow());
    entry.put("h1", page.h1());
    entry.put("lead", page.lead());
    entry.put("homePage", page.homePage());
    entry.put("seoHtml", buildSeoContent(page, removeBgEnabled));
    return entry;
  }

  private static String toSafeScriptJson(Object payload) {
    try {
      return JSON.writeValueAsString(payload).replace("</", "<\\/");
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Falha ao serializar metadados JSON da página.", exception);
    }
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(Objects.requireNonNullElse(value, ""), StandardCharsets.UTF_8.name());
  }

  private static String loadTemplate(String resourcePath) {
    ClassPathResource resource = new ClassPathResource(resourcePath);
    try (InputStream stream = resource.getInputStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException("Não foi possível carregar o template HTML: " + resourcePath, exception);
    }
  }
}
