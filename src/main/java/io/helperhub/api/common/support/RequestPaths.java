package io.helperhub.api.common.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

public final class RequestPaths {
  private RequestPaths() {}

  public static String applicationPath(HttpServletRequest request) {
    // Match decoded routes and remove matrix parameters before applying resource limits.
    return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
  }
}
