package id.my.hendisantika.multitenancy.service.email;

/**
 * The invitation message. Kept as plain string building rather than a template
 * engine: there is one message, and it is easier to see what a recipient gets.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.55
 */
public final class InvitationEmail {

    private InvitationEmail() {
    }

    public static EmailSender.EmailMessage build(String to, String organizationName,
                                                 String role, String acceptUrl, long daysValid) {
        String organization = escape(organizationName);
        String subject = "You have been invited to join " + organizationName;

        String html = """
                <div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1c2333">
                  <h1 style="font-size:20px;margin:0 0 8px">Join %s</h1>
                  <p style="margin:0 0 20px;color:#5b6472;font-size:14px;line-height:1.5">
                    You have been invited as %s. Open the link below to choose your own password and join.
                    Nobody else, including whoever invited you, ever sees it.
                  </p>
                  <p style="margin:0 0 24px">
                    <a href="%s" style="display:inline-block;background:#256AD6;color:#fff;text-decoration:none;padding:10px 18px;border-radius:8px;font-size:14px;font-weight:500">Accept invitation</a>
                  </p>
                  <p style="margin:0 0 8px;color:#5b6472;font-size:12px">
                    The link expires in %d days and can be used once. If the button does not work, paste this into your browser:
                  </p>
                  <p style="margin:0;color:#5b6472;font-size:12px;word-break:break-all">%s</p>
                </div>
                """.formatted(organization, escape(role), acceptUrl, daysValid, acceptUrl);

        String text = """
                Join %s

                You have been invited as %s. Open the link below to choose your own password and join.
                Nobody else, including whoever invited you, ever sees it.

                %s

                The link expires in %d days and can be used once.
                """.formatted(organizationName, role, acceptUrl, daysValid);

        return new EmailSender.EmailMessage(to, subject, html, text);
    }

    /**
     * An organization name is user supplied and lands inside HTML, so it cannot
     * go in raw.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
