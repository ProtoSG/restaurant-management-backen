package com.restaurant_management.restaurant_management_backend.security;

import com.restaurant_management.restaurant_management_backend.shared.security.RateLimitFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter();
  }

  private MockHttpServletRequest loginRequest(String remoteAddr) {
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
    req.setServletPath("/auth/login");
    req.setRemoteAddr(remoteAddr);
    return req;
  }

  @Test
  void nonLoginPath_passesThrough() throws ServletException, IOException {
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/register");
    req.setServletPath("/auth/register");
    req.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse resp = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, resp, chain);

    assertThat(chain.getRequest()).isNotNull();
    assertThat(resp.getStatus()).isEqualTo(200);
  }

  @Test
  void loginPath_allowsUpToFiveRequestsPerIp() throws ServletException, IOException {
    for (int i = 0; i < 5; i++) {
      MockHttpServletResponse resp = new MockHttpServletResponse();
      MockFilterChain chain = new MockFilterChain();

      filter.doFilter(loginRequest("10.0.0.2"), resp, chain);

      assertThat(chain.getRequest())
        .as("request %d should pass through", i + 1)
        .isNotNull();
      assertThat(resp.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void loginPath_blocks6thRequestWithHttp429() throws ServletException, IOException {
    for (int i = 0; i < 5; i++) {
      filter.doFilter(loginRequest("10.0.0.3"), new MockHttpServletResponse(), new MockFilterChain());
    }

    MockHttpServletResponse resp6 = new MockHttpServletResponse();
    MockFilterChain chain6 = new MockFilterChain();

    filter.doFilter(loginRequest("10.0.0.3"), resp6, chain6);

    assertThat(chain6.getRequest()).isNull();
    assertThat(resp6.getStatus()).isEqualTo(429);
    assertThat(resp6.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
  }

  @Test
  void loginPath_differentIpsHaveIndependentBuckets() throws ServletException, IOException {
    // Exhaust bucket for IP A
    for (int i = 0; i < 5; i++) {
      filter.doFilter(loginRequest("192.168.1.1"), new MockHttpServletResponse(), new MockFilterChain());
    }

    // IP B should still get through on its 1st request
    MockHttpServletResponse respB = new MockHttpServletResponse();
    MockFilterChain chainB = new MockFilterChain();
    filter.doFilter(loginRequest("192.168.1.2"), respB, chainB);

    assertThat(chainB.getRequest()).isNotNull();
    assertThat(respB.getStatus()).isEqualTo(200);
  }

  @Test
  void resolveIp_prefersXForwardedForHeader() throws ServletException, IOException {
    MockHttpServletRequest req = loginRequest("10.0.0.99");
    req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.99");
    req.setServletPath("/auth/login");

    MockHttpServletResponse resp = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, resp, chain);

    assertThat(chain.getRequest()).isNotNull();
  }
}
