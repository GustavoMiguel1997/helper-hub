package io.helperhub.api.common;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.audio.config.*;
import io.helperhub.api.image.config.*;
import io.helperhub.api.video.config.*;
import io.helperhub.api.common.support.ClientRateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.filter.OncePerRequestFilter;

class RateLimitTest {
  @Test void forwardedHeaderCannotBypassAnyDomainLimit() throws Exception {
    var audio = new AudioUsageProperties(); audio.setMaxRequestsPerMinute(1);
    var image = new ImageUsageProperties(); image.setMaxRequestsPerMinute(1);
    var video = new VideoUsageProperties(); video.setMaxRequestsPerMinute(1);
    check(new AudioRateLimitFilter(audio), "/api/audio/cut");
    check(new ImageRateLimitFilter(image), "/api/image/resize");
    check(new VideoRateLimitFilter(video), "/api/video/youtube-download");
  }

  private void check(OncePerRequestFilter filter, String path) throws Exception {
    for (int i = 0; i < 2; i++) {
      var request = new MockHttpServletRequest("POST", path);
      request.setRemoteAddr("192.0.2.1");
      request.addHeader("X-Forwarded-For", "192.0.2." + (10 + i));
      var response = new MockHttpServletResponse();
      filter.doFilter(request, response, new MockFilterChain());
      assertEquals(i == 0 ? 200 : 429, response.getStatus());
      if (i == 1) assertEquals("60", response.getHeader("Retry-After"));
    }
  }

  @Test void fullCacheDoesNotResetLimitsAndExpiredEntriesAreReclaimed() {
    AtomicLong clock = new AtomicLong();
    var limiter = new ClientRateLimiter(1, clock::get);
    assertTrue(limiter.tryConsume("first", 1));
    assertFalse(limiter.tryConsume("second", 1));
    assertFalse(limiter.tryConsume("first", 1));
    clock.addAndGet(Duration.ofMinutes(1).toNanos());
    assertTrue(limiter.tryConsume("second", 1));
  }
}
