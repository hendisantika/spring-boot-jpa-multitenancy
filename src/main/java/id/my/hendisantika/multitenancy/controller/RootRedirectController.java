package id.my.hendisantika.multitenancy.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Sends the root of the API host to the Swagger UI, so opening
 * api-dev.jvm.my.id lands on the documentation rather than a 404.
 * <p>
 * The Location is path-only (/swagger-ui), so the browser resolves it against
 * whatever scheme and host it arrived on — no dependence on forwarded headers
 * behind the proxy. Hidden from the OpenAPI document: it is navigation, not API.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@Hidden
@RestController
public class RootRedirectController {

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui"))
                .build();
    }
}
