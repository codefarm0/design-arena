package in.codefarm.designarena.config;

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import in.codefarm.designarena.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

/**
 * Who may see what.
 *
 * <p>There is no list of URL patterns here. Vaadin derives access from annotations on the
 * views themselves — {@code @AnonymousAllowed} or {@code @PermitAll} — which keeps the
 * rule next to the screen it protects, where somebody adding a screen will see it. The
 * default for an unannotated view is to deny, so forgetting to decide is a locked door
 * rather than an open one.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.with(
                        vaadin(),
                        configurer -> configurer
                                .loginView(LoginView.class, "/")
                                // Authorisation is decided per view by the annotations,
                                // not per URL here. Leaving the HTTP layer's default of
                                // authenticated() in place sends an anonymous visitor to
                                // the login page before any view annotation is consulted
                                // - which is exactly what happened the first time this
                                // was wired up: even the public catalogue redirected.
                                .anyRequest(AuthorizedUrl::permitAll))
                .build();
    }

    /**
     * Turns on the checker that reads {@code @AnonymousAllowed}, {@code @PermitAll} and
     * {@code @RolesAllowed} from the views themselves.
     *
     * <p>Without this bean those annotations are inert. With it, a view carrying no
     * annotation at all is denied, so adding a screen and forgetting to decide who may
     * see it produces a locked door rather than an open one.
     */
    @Bean
    NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
        return new NavigationAccessControlConfigurer()
                .withAnnotatedViewAccessChecker()
                .withLoginView(LoginView.class);
    }

    /**
     * BCrypt, with its default strength.
     *
     * <p>A deliberately slow hash is the point: it makes an offline attack on a stolen
     * password table expensive rather than instant.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
