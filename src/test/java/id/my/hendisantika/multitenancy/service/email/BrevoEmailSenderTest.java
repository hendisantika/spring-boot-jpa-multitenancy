package id.my.hendisantika.multitenancy.service.email;

import id.my.hendisantika.multitenancy.config.BrevoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Checks the request actually put on the wire, since the live API cannot be
 * reached from a build.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.55
 */
class BrevoEmailSenderTest {

    private static final String API_KEY = "xkeysib-not-a-real-key";

    private MockRestServiceServer server;
    private BrevoEmailSender sender;
    private BrevoProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BrevoProperties();
        properties.setApiKey(API_KEY);
        properties.setSenderEmail("no-reply@jvm.my.id");
        properties.setSenderName("Multitenancy");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("api-key", properties.getApiKey());
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoEmailSender(builder.build(), properties);
    }

    private EmailSender.EmailMessage message() {
        return InvitationEmail.build(
                "nurse@example.test", "Klinik Sehat", "MEMBER",
                "https://app.example/invitations/tok3n", 7);
    }

    @Test
    void postsTheMessageToBrevoWithTheApiKeyHeader() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.to[0].email").value("nurse@example.test"))
                .andExpect(jsonPath("$.sender.email").value("no-reply@jvm.my.id"))
                .andExpect(jsonPath("$.subject").value("You have been invited to join Klinik Sehat"))
                .andExpect(jsonPath("$.htmlContent").exists())
                .andExpect(jsonPath("$.textContent").exists())
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"<1@brevo>\"}"));

        assertThat(sender.send(message())).isTrue();
        server.verify();
    }

    /**
     * A mail failure must not throw: the invitation is already created, and
     * losing it would be worse than an undelivered message.
     */
    @Test
    void reportsFailureWithoutThrowing() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withServerError());

        assertThat(sender.send(message())).isFalse();
        server.verify();
    }

    @Test
    void isEnabledOnlyWithAnApiKey() {
        assertThat(sender.isEnabled()).isTrue();

        properties.setApiKey("   ");
        assertThat(sender.isEnabled()).isFalse();
    }

    /**
     * The organization name is user supplied and lands inside HTML.
     */
    @Test
    void escapesTheOrganizationNameInTheHtml() {
        EmailSender.EmailMessage message = InvitationEmail.build(
                "nurse@example.test", "<script>alert(1)</script>", "MEMBER",
                "https://app.example/invitations/tok3n", 7);

        assertThat(message.html()).doesNotContain("<script>");
        assertThat(message.html()).contains("&lt;script&gt;");
    }

    @Test
    void bothPartsCarryTheLink() {
        EmailSender.EmailMessage message = message();

        assertThat(message.html()).contains("https://app.example/invitations/tok3n");
        assertThat(message.text()).contains("https://app.example/invitations/tok3n");
        assertThat(message.text()).contains("expires in 7 days");
    }
}
