package id.my.hendisantika.multitenancy.controller;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A page of results in a shape this project owns.
 * <p>
 * Serialising Spring Data's own {@code Page} would tie the JSON to an internal
 * class, which Spring Data itself warns against, so the fields a client
 * actually needs are named here instead.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 14.02
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
