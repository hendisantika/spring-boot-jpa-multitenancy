package id.my.hendisantika.multitenancy.support;

import id.my.hendisantika.multitenancy.config.TenantContext;
import id.my.hendisantika.multitenancy.entity.Tenant;

/**
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
    private Tenant tenant = null;

    public TenantAwareThread(Runnable target) {
        super(target);
        this.tenant = TenantContext.getTenant();
    }

    @Override
    public void run() {
        TenantContext.setTenant(this.tenant);
        super.run();
        TenantContext.clearTenant();
    }
}
