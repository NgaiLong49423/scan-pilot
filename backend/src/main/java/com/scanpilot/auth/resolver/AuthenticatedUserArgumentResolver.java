package com.scanpilot.auth.resolver;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.dto.UserProfileDto;
import com.scanpilot.auth.interceptor.AuthInterceptor;
import com.scanpilot.auth.model.UserSession;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                || parameter.getParameterType().equals(UserSession.class)
                || parameter.getParameterType().equals(UserProfileDto.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        UserSession session = (UserSession) webRequest.getAttribute(
                AuthInterceptor.ATTR_USER_SESSION,
                RequestAttributes.SCOPE_REQUEST
        );

        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (annotation != null && annotation.required() && session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (parameter.getParameterType().equals(UserProfileDto.class)) {
            return session != null ? UserProfileDto.from(session) : null;
        }

        return session;
    }
}
