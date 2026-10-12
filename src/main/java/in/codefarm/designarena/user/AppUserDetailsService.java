package in.codefarm.designarena.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * How Spring Security looks a person up at sign-in.
 *
 * <p>Everyone gets {@code ROLE_USER}; an administrator gets {@code ROLE_ADMIN} as well,
 * so the same account can edit a rubric and then take a quiz without signing in twice.
 */
@Service
class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        AppUser user = users.findByEmailIgnoringCase(email)
                // The message deliberately does not say whether the address exists:
                // telling an attacker which emails are registered is a free gift.
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
        // Everyone is a USER; administrators are additionally an ADMIN, so an admin can
        // still use the product as a normal person without a second account.
        String[] roles = user.getRole() == UserRole.ADMIN ? new String[] {"USER", "ADMIN"} : new String[] {"USER"};
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(roles)
                .build();
    }
}
