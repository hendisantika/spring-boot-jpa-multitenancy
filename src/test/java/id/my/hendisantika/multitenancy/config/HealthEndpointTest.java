package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The container HEALTHCHECK probes this without credentials, so it has to answer
 * unauthenticated, and it must not leak anything while doing so.
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
     * Only health is exposed; anything else must not be served at all.
     */
    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mvc().perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc().perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
    }
}
