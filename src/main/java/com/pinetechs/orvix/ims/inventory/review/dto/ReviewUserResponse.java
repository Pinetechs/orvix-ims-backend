package com.pinetechs.orvix.ims.inventory.review.dto;

import com.pinetechs.orvix.ims.user.entity.User;

public record ReviewUserResponse(
        Long id,
        String username,
        String displayName
) {
    public static ReviewUserResponse from(User user) {
        if (user == null) return null;
        return new ReviewUserResponse(user.getId(), user.getUsername(), user.getFullName());
    }
}
