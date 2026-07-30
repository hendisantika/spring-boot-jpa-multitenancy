package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.JwtProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Issues the access and refresh tokens for the parent login.
 * <p>
 * The access token carries the tenants the account may reach, so a request to a
 * tenant subdomain can be authorised without a database round trip.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_MEMBERSHIPS = "memberships";
    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String issueAccessToken(Account account, List<UserTenant> memberships) {
        Map<String, String> claims = memberships.stream()
                .collect(Collectors.toMap(UserTenant::getTenantSlug, m -> m.getRole().name(), (a, b) -> a));
        return encode(account, TYPE_ACCESS, jwtProperties.getAccessTokenTtl(), claims);
    }

    /**
     * Refresh tokens deliberately carry no memberships: they are only good for
     * minting a new access token, whose memberships are read fresh from the
     * database at that moment.
     */
    public String issueRefreshToken(Account account) {
        return encode(account, TYPE_REFRESH, jwtProperties.getRefreshTokenTtl(), Map.of());
    }

    private String encode(Account account, String type, Duration ttl, Map<String, String> memberships) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(String.valueOf(account.getId()))
                .claim(CLAIM_EMAIL, account.getEmail())
                .claim(CLAIM_TOKEN_TYPE, type)
                .claim(CLAIM_MEMBERSHIPS, memberships)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
