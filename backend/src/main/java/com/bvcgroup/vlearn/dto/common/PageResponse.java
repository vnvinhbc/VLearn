package com.bvcgroup.vlearn.dto.common;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class PageResponse<T> {

    private List<T> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private List<String> sort;

    public static <T> PageResponse<T> from(Page<T> page) {
        List<String> sort = page.getSort().stream()
                .map(order -> order.getProperty() + ": " + order.getDirection())
                .toList();

        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .sort(sort)
                .build();
    }
}
