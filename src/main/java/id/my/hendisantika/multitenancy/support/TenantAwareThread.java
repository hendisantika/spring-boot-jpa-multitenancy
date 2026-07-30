package id.my.hendisantika.multitenancy.support;

import id.my.hendisantika.multitenancy.config.TenantContext;

/**
 * Carries the current tenant into a new thread, which a plain ThreadLocal does not
 * inherit.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:42
 * To change this template use File | Settings | File Templates.
 */
public class TenantAwareThread extends Thread {

    private final String tenant;

    public TenantAwareThread(Runnable target) {
        super(target);
        this.tenant = TenantContext.getTenant();
    }

    @Override
    public void run() {
        TenantContext.setTenant(this.tenant);
        try {
            super.run();
        } finally {
            TenantContext.clearTenant();
        }
    }
}
