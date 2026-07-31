package id.my.hendisantika.multitenancy.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Single-use link tokens, shared by invitations, password resets and email
 * verification. All three hand the raw token out once and keep only its hash, so
 * a leaked database yields no working links.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.18
 */
public final class SecureTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 256 bits, so guessing is not a threat worth rate limiting for.
     */
    private static final int TOKEN_BYTES = 32;

    private SecureTokens() {
    }

    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * @return lowercase hex SHA-256, which is what gets stored
     */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /**
     * Appends the token to a base url, tolerating a trailing slash either way.
     */
    public static String linkTo(String baseUrl, String token) {
        return (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + token;
    }
}
