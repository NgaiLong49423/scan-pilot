package com.scanpilot.auth.dto;

import com.scanpilot.auth.model.UserSession;

/**
 * Public user profile returned to the frontend.
 * Strictly omits server-only fields such as accessToken.
 */
public record UserProfileDto(
        Long githubUserId,
        String login,
        String name,
        String avatarUrl,
        String email
) {
    public static UserProfileDto from(UserSession session) {
        if (session == null) {
            return null;
        }
        return new UserProfileDto(
                session.getGithubUserId(),
                session.getLogin(),
                session.getName(),
                session.getAvatarUrl(),
                session.getEmail()
        );
    }
}
