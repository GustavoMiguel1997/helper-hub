package io.helperhub.api.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class ProcessingLimitFilter extends OncePerRequestFilter {
  private final Semaphore permits;

  public ProcessingLimitFilter(@Value("${helperhub.processing.max-concurrent:4}") int maximum) {
    permits = new Semaphore(Math.max(1, maximum));
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !request.getMethod().equals("POST") || !(path.startsWith("/api/audio/")
        || path.startsWith("/api/image/") || path.startsWith("/api/video/"));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    if (!permits.tryAcquire()) {
      response.setStatus(503);
      response.setHeader("Retry-After", "5");
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write("{\"error\":\"PROCESSING_BUSY\",\"message\":\"Servidor ocupado. Tente novamente em alguns segundos.\"}");
      return;
    }
    try { chain.doFilter(request, response); } finally { permits.release(); }
  }
}
