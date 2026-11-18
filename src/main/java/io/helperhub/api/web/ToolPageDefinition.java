package io.helperhub.api.web;

import java.util.List;

/**
 * Metadados completos e conteúdo indexável de uma página web do HelperHub.
 */
public record ToolPageDefinition(
    String path,
    String toolKey,
    String category,
    String navLabel,
    String title,
    String metaDescription,
    String eyebrow,
    String h1,
    String lead,
    String summaryHeading,
    List<String> summaryParagraphs,
    List<FaqItem> faqs,
    boolean homePage
) {

  public record FaqItem(String question, String answer) {}
}
