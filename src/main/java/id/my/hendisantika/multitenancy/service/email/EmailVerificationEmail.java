package id.my.hendisantika.multitenancy.service.email;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.18
 */
public final class EmailVerificationEmail {

    private EmailVerificationEmail() {
    }

    public static EmailSender.EmailMessage build(String to, String verifyUrl, long hoursValid) {
        String subject = "Confirm your email address";

        String html = """
                <div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1c2333">
                  <h1 style="font-size:20px;margin:0 0 8px">Confirm your email address</h1>
                  <p style="margin:0 0 20px;color:#5b6472;font-size:14px;line-height:1.5">
                    You can sign in already. Confirming unlocks registering an organization.
                  </p>
                  <p style="margin:0 0 24px">
                    <a href="%s" style="display:inline-block;background:#256AD6;color:#fff;text-decoration:none;padding:10px 18px;border-radius:8px;font-size:14px;font-weight:500">Confirm email</a>
                  </p>
                  <p style="margin:0 0 8px;color:#5b6472;font-size:12px">
                    The link expires in %d hours. If the button does not work, paste this into your browser:
                  </p>
                  <p style="margin:0;color:#5b6472;font-size:12px;word-break:break-all">%s</p>
                </div>
                """.formatted(verifyUrl, hoursValid, verifyUrl);

        String text = """
                Confirm your email address

                You can sign in already. Confirming unlocks registering an organization.

                %s

                The link expires in %d hours.
                """.formatted(verifyUrl, hoursValid);

        return new EmailSender.EmailMessage(to, subject, html, text);
    }
}
