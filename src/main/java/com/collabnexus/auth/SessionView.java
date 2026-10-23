package com.collabnexus.auth;

import com.collabnexus.brands.BrandView;
import com.collabnexus.creators.CreatorView;
import com.collabnexus.users.User;

import java.time.Instant;

public record SessionView(
        boolean authenticated,
        UserView user,
        BrandView brand,
        CreatorView creator
) {

    public static SessionView of(User user, BrandView brand, CreatorView creator) {
        return new SessionView(true, UserView.from(user), brand, creator);
    }

    public record UserView(
            Long id,
            String email,
            String fullName,
            String role,
            String status,
            Instant createdAt
    ) {
        public static UserView from(User user) {
            return new UserView(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.getStatus().name(),
                    user.getCreatedAt()
            );
        }
    }
}
