package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The actuator endpoints answer without credentials — the container HEALTHCHECK,
 * an orchestrator and anyone reading which build is live all reach them
 * anonymously — and opening them must not open the secrets behind them.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 07.04
 */
@SpringBootTest
class HealthEndpointTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    void healthIsReachableWithoutATokenAndReportsUp() throws Exception {
        mvc().perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void livenessAndReadinessAnswerSeparately() throws Exception {
        mvc().perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc().perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * show-details is "when-authorized", so an anonymous probe learns whether the
     * application is up but nothing about the database behind it.
     */
    @Test
    void healthHidesItsDetailsFromAnAnonymousCaller() throws Exception {
        mvc().perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    /**
     * Info, and every other actuator endpoint, is exposed and anonymous.
     */
    @Test
    void actuatorEndpointsAreReachableWithoutAToken() throws Exception {
        mvc().perform(get("/actuator/info")).andExpect(status().isOk());
        mvc().perform(get("/actuator/beans")).andExpect(status().isOk());
        mvc().perform(get("/actuator/env")).andExpect(status().isOk());
    }

    /**
     * Opening the endpoints does not open the credentials: env values for keys
     * that name a secret are sanitized, so an anonymous caller reads the shape of
     * the configuration but none of the values that would let them in.
     */
    @Test
    void envDoesNotLeakSecretsToAnAnonymousCaller() throws Exception {
        // The development JWT secret is the literal committed to
        // application.properties; it must not appear in the response body.
        mvc().perform(get("/actuator/env"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("change-this-development-only-secret-please-32b"))));
        mvc().perform(get("/actuator/env/application.jwt.secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.property.value").value("******"));
    }

    /**
     * The OpenAPI document backing the Swagger UI is public, so the UI can load
     * it without a token.
     */
    @Test
    void theOpenApiDocumentIsPublic() throws Exception {
        mvc().perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }
}
