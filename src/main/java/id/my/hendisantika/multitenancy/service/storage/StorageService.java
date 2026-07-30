package id.my.hendisantika.multitenancy.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded files. The account row keeps the returned key, never a URL, so
 * the bucket or the endpoint can change without touching stored data.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public interface StorageService {

    /**
     * @param prefix logical folder, for example "accounts"
     * @return the stored object key
     */
    String store(MultipartFile file, String prefix);

    /**
     * @return a URL a client can fetch the object from
     */
    String urlOf(String key);

    void delete(String key);
}
