package id.my.hendisantika.multitenancy.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;

/**
 * Stateless security for the parent login. Signup and login are open; everything
 * else needs a bearer token issued by {@code TokenService}.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Slf4j
@Configuration
// Lets the tenant data rules sit on the methods they guard, rather than being
// repeated at the top of each one.
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {

    /**
     * Running under any of these means production rules apply, so the development
     * secret is refused.
     */
    private static final String[] PRODUCTION_PROFILES = {"prod", "production", "staging"};

    private final Environment environment;

    public SecurityConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Tokens are sent in the Authorization header, never in a cookie, so
                // there is no CSRF surface to protect.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                        // The container HEALTHCHECK and any orchestrator probe this
                        // without credentials. Details stay hidden unless authorized,
                        // so it only ever reveals UP or DOWN.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Accepting an invitation happens before the recipient has
                        // an account: the token in the link is the only credential.
                        .requestMatchers(HttpMethod.GET, "/api/invitations/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/invitations/*/accept").permitAll()
                        // Resetting a password happens when you cannot sign in.
                        .requestMatchers(HttpMethod.POST, "/api/auth/password/forgot").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/password/reset/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password/reset/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-email/*").permitAll()
                        // Confirming a new address happens in whichever browser
                        // reads that mailbox, which may hold no session at all.
                        .requestMatchers(HttpMethod.POST, "/api/auth/email-change/*").permitAll()
                        // Every other actuator endpoint stays behind a token.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(jwtProperties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder.withSecretKey(signingKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKey signingKey(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        SecretKey key = JwtSecretPolicy.signingKey(secret, isProduction());
        if (JwtSecretPolicy.isDevelopmentSecret(secret)) {
            // Loud even outside production: anything reachable by others is signing
            // tokens with a key that is public in this repository.
            log.warn("application.jwt.secret is the development value from application.properties. "
                    + "Anyone can mint a token for any account. Set APPLICATION_JWT_SECRET before exposing "
                    + "this instance to anyone.");
        }
        return key;
    }

    private boolean isProduction() {
        return environment.matchesProfiles(PRODUCTION_PROFILES);
    }
}
