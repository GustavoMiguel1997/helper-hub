package io.helperhub.api.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.helperhub.api.image.config.ImageUsageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WebPageController.class)
@Import(ToolPageRenderer.class)
@EnableConfigurationProperties(ImageUsageProperties.class)
@TestPropertySource(properties = "helperhub.image.remove-bg-api-key=")
class WebPageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void homePageContainsToolLinksAndCanonicalMetadata() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"https://helperhub.gmslab.cloud/\">")))
        .andExpect(content().string(containsString("<meta name=\"robots\" content=\"index, follow\">")))
        .andExpect(content().string(containsString("data-initial-tool=\"explore\"")))
        .andExpect(content().string(containsString("href=\"/comprimir-imagem\"")))
        .andExpect(content().string(containsString("href=\"/cortar-audio\"")))
        .andExpect(content().string(containsString("href=\"/extrair-audio-video\"")));
  }

  @Test
  void dedicatedToolPagesReturnCustomSeoMetadataAndInitialTool() throws Exception {
    for (ToolPageDefinition tool : ToolPageCatalog.pages()) {
      if ("removeBg".equals(tool.toolKey())) {
        continue;
      }
      mockMvc.perform(get(tool.path()))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString(
              "<link rel=\"canonical\" href=\"https://helperhub.gmslab.cloud" + tool.path() + "\">")))
          .andExpect(content().string(containsString("<meta name=\"robots\" content=\"index, follow\">")))
          .andExpect(content().string(containsString("data-initial-tool=\"" + tool.toolKey() + "\"")))
          .andExpect(content().string(containsString("<h1 id=\"page-h1\">" + tool.h1() + "</h1>")))
          .andExpect(content().string(containsString("\"@type\":\"FAQPage\"")));
    }
  }

  @Test
  void unconfiguredRemoveBgPageIsMarkedNoindex() throws Exception {
    mockMvc.perform(get("/remover-fundo-imagem"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex, follow\">")))
        .andExpect(content().string(containsString("data-initial-tool=\"removeBg\"")));
  }

  @Test
  void toolBreadcrumbMatchesVisiblePageAndCanonical() throws Exception {
    String html = mockMvc.perform(get("/comprimir-imagem-500kb"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    var script = java.util.regex.Pattern.compile(
        "<script type=\"application/ld\\+json\">(.*?)</script>",
        java.util.regex.Pattern.DOTALL).matcher(html);
    assertTrue(script.find());
    var graph = new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(script.group(1)).get("@graph");
    var breadcrumb = java.util.stream.StreamSupport.stream(graph.spliterator(), false)
        .filter(node -> "BreadcrumbList".equals(node.path("@type").asText()))
        .findFirst().orElseThrow();
    var items = breadcrumb.get("itemListElement");
    assertEquals(2, items.size());
    assertEquals("https://helperhub.gmslab.cloud/", items.get(0).get("item").asText());
    assertEquals(1, items.get(0).get("position").asInt());
    assertEquals("https://helperhub.gmslab.cloud/comprimir-imagem-500kb", items.get(1).get("item").asText());
    assertEquals("Comprimir imagem para 500 KB", items.get(1).get("name").asText());
    assertTrue(html.contains("<nav class=\"breadcrumb\""));
  }

  @Test
  void sitemapListsHomeAndActiveToolsOnly() throws Exception {
    mockMvc.perform(get("/sitemap.xml"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/xml"))
        .andExpect(content().string(containsString("<loc>https://helperhub.gmslab.cloud/</loc>")))
        .andExpect(content().string(containsString("<loc>https://helperhub.gmslab.cloud/comprimir-imagem</loc>")))
        .andExpect(content().string(containsString("<loc>https://helperhub.gmslab.cloud/comprimir-imagem-500kb</loc>")))
        .andExpect(content().string(containsString("<loc>https://helperhub.gmslab.cloud/converter-png-para-jpg</loc>")))
        .andExpect(content().string(containsString("<loc>https://helperhub.gmslab.cloud/extrair-audio-video</loc>")))
        .andExpect(content().string(not(containsString("/remover-fundo-imagem"))));
  }
}
