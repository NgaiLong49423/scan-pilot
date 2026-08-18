package com.scanpilot.auth.interceptor;

import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_SESSION = "scanpilot.auth.userSession";

    private final SessionService sessionService;
    private final AuthConfigProperties properties;

    public AuthInterceptor(SessionService sessionService, AuthConfigProperties properties) {
        this.sessionService = sessionService;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserSession session = resolveSession(request);
        if (session != null) {
            request.setAttribute(ATTR_USER_SESSION, session);
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            boolean requireAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);

            if (requireAuth && session == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                return false;
            }
        }

        return true;
    }

    private UserSession resolveSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(c -> properties.getCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(val -> val != null && !val.isBlank())
                .findFirst()
                .flatMap(sessionService::getSession)
                .orElse(null);
    }
}
