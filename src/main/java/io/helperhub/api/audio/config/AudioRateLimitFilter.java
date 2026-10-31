package io.helperhub.api.audio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.helperhub.api.common.exception.ApiError;
import io.helperhub.api.common.support.ClientRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AudioRateLimitFilter extends OncePerRequestFilter {

  private static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";



  private static final ObjectMapper RATE_LIMIT_JSON = createRateLimitObjectMapper();

  private final AudioUsageProperties audioUsageProperties;
  private final ClientRateLimiter limiter = new ClientRateLimiter();

  public AudioRateLimitFilter(AudioUsageProperties audioUsageProperties) {
    this.audioUsageProperties = audioUsageProperties;
  }

  private static ObjectMapper createRateLimitObjectMapper() {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!audioUsageProperties.isRateLimitEnabled()) {
      return true;
    }
    String uri = request.getRequestURI();
    String contextPath = request.getContextPath();
    String prefix = contextPath.isEmpty() ? "" : contextPath;
    return !uri.startsWith(prefix + "/api/audio/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (!limiter.tryConsume(request.getRemoteAddr(), audioUsageProperties.getMaxRequestsPerMinute())) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader("Retry-After", "60");
      response.setCharacterEncoding("UTF-8");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      ApiError body = ApiError.of(
          RATE_LIMIT_EXCEEDED,
          "Limite de uso excedido. Aguarde antes de enviar novas requisições para a API de áudio.");
      RATE_LIMIT_JSON.writeValue(response.getWriter(), body);
      return;
    }
    filterChain.doFilter(request, response);
  }

}
