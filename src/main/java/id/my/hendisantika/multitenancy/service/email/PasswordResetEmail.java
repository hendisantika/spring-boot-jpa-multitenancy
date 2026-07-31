package id.my.hendisantika.multitenancy.service.email;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.05
 */
public final class PasswordResetEmail {

    private PasswordResetEmail() {
    }

    public static EmailSender.EmailMessage build(String to, String resetUrl, long minutesValid) {
        String subject = "Reset your password";

        String html = """
                <div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1c2333">
                  <h1 style="font-size:20px;margin:0 0 8px">Reset your password</h1>
                  <p style="margin:0 0 20px;color:#5b6472;font-size:14px;line-height:1.5">
                    Open the link below to choose a new one. If you did not ask for this, you can ignore
                    this message: nothing changes until the link is used.
                  </p>
                  <p style="margin:0 0 24px">
                    <a href="%s" style="display:inline-block;background:#256AD6;color:#fff;text-decoration:none;padding:10px 18px;border-radius:8px;font-size:14px;font-weight:500">Choose a new password</a>
                  </p>
                  <p style="margin:0 0 8px;color:#5b6472;font-size:12px">
                    The link expires in %d minutes and can be used once. If the button does not work, paste this into your browser:
                  </p>
                  <p style="margin:0;color:#5b6472;font-size:12px;word-break:break-all">%s</p>
                </div>
                """.formatted(resetUrl, minutesValid, resetUrl);

        String text = """
                Reset your password

                Open the link below to choose a new one. If you did not ask for this, you can ignore
                this message: nothing changes until the link is used.

                %s

                The link expires in %d minutes and can be used once.
                """.formatted(resetUrl, minutesValid);

        return new EmailSender.EmailMessage(to, subject, html, text);
    }
}
