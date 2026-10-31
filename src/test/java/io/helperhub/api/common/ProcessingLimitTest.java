package io.helperhub.api.common;

import static org.junit.jupiter.api.Assertions.*;
import io.helperhub.api.common.config.ProcessingLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class ProcessingLimitTest {
  @Test void rejectsConcurrentRequestAndReleasesPermitAfterFailure() throws Exception {
    var filter = new ProcessingLimitFilter(1);
    assertThrows(jakarta.servlet.ServletException.class, () -> filter.doFilter(
        new MockHttpServletRequest("POST", "/api/image/resize"), new MockHttpServletResponse(), (req, res) -> {
          var second = new MockHttpServletResponse();
          filter.doFilter(new MockHttpServletRequest("POST", "/api/audio/cut"), second, new MockFilterChain());
          assertEquals(503, second.getStatus());
          throw new jakarta.servlet.ServletException("simulated failure");
        }));
    var response = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest("POST", "/api/image/resize"), response, new MockFilterChain());
    assertEquals(200, response.getStatus());
  }
}
