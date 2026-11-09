package com.bvcgroup.vlearn.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.bvcgroup.vlearn.constant.ApiConstants;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageResponseTest {

    @Test
    void paginationContract_defaultsAndMaximumRemainStable() {
        assertThat(ApiConstants.DEFAULT_PAGE_SIZE).isEqualTo(20);
        assertThat(ApiConstants.MAX_PAGE_SIZE).isEqualTo(100);
    }

    @Test
    void from_pageWithStableSort_copiesPaginationMetadata() {
        PageImpl<String> page = new PageImpl<>(
                List.of("third", "fourth"),
                PageRequest.of(1, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))),
                5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.getContent()).containsExactly("third", "fourth");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getSort()).containsExactly("createdAt: DESC", "id: ASC");
    }
}
