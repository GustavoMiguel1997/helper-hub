package io.helperhub.api.video.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.helperhub.api.common.exception.ApiError;
import io.helperhub.api.common.support.ClientRateLimiter;
import io.helperhub.api.common.support.RequestPaths;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 22)
public class VideoRateLimitFilter extends OncePerRequestFilter {

  private static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

  private static final ObjectMapper RATE_LIMIT_JSON = createRateLimitObjectMapper();

  private final VideoUsageProperties videoUsageProperties;
  private final ClientRateLimiter limiter = new ClientRateLimiter();

  public VideoRateLimitFilter(VideoUsageProperties videoUsageProperties) {
    this.videoUsageProperties = videoUsageProperties;
  }

  private static ObjectMapper createRateLimitObjectMapper() {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!videoUsageProperties.isRateLimitEnabled()) {
      return true;
    }
    return !RequestPaths.applicationPath(request).startsWith("/api/video/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (!limiter.tryConsume(request.getRemoteAddr(), videoUsageProperties.getMaxRequestsPerMinute())) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader("Retry-After", "60");
      response.setCharacterEncoding("UTF-8");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      ApiError body = ApiError.of(
          RATE_LIMIT_EXCEEDED,
          "Limite de uso excedido. Aguarde antes de enviar novas requisições para a API de vídeo.");
      RATE_LIMIT_JSON.writeValue(response.getWriter(), body);
      return;
    }
    filterChain.doFilter(request, response);
  }

}
