package id.my.hendisantika.multitenancy.service.email;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 08.29
 */
public final class EmailChangeEmail {

    private EmailChangeEmail() {
    }

    /**
     * Goes to the new address, not the current one: the point is to find out
     * whether whoever asked can read the mailbox they named.
     *
     * @param currentEmail named in the body so that somebody who was sent this
     *                     by mistake can see which account is involved
     */
    public static EmailSender.EmailMessage build(String to, String currentEmail, String confirmUrl, long hoursValid) {
        String subject = "Confirm your new email address";

        String html = """
                <div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1c2333">
                  <h1 style="font-size:20px;margin:0 0 8px">Confirm your new email address</h1>
                  <p style="margin:0 0 20px;color:#5b6472;font-size:14px;line-height:1.5">
                    The account currently signing in as %s asked to move to this address.
                    Nothing changes until you confirm; until then %s still signs in.
                  </p>
                  <p style="margin:0 0 24px">
                    <a href="%s" style="display:inline-block;background:#256AD6;color:#fff;text-decoration:none;padding:10px 18px;border-radius:8px;font-size:14px;font-weight:500">Confirm new address</a>
                  </p>
                  <p style="margin:0 0 8px;color:#5b6472;font-size:12px">
                    The link expires in %d hours. If you did not ask for this, ignore it — the
                    address stays as it is. If the button does not work, paste this into your browser:
                  </p>
                  <p style="margin:0;color:#5b6472;font-size:12px;word-break:break-all">%s</p>
                </div>
                """.formatted(currentEmail, currentEmail, confirmUrl, hoursValid, confirmUrl);

        String text = """
                Confirm your new email address

                The account currently signing in as %s asked to move to this address.
                Nothing changes until you confirm; until then %s still signs in.

                %s

                The link expires in %d hours. If you did not ask for this, ignore it.
                """.formatted(currentEmail, currentEmail, confirmUrl, hoursValid);

        return new EmailSender.EmailMessage(to, subject, html, text);
    }
}
