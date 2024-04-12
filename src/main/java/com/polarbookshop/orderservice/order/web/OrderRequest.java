package com.polarbookshop.orderservice.order.web;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record OrderRequest(
        @NotBlank(message = "ISBN 은 필수입니다")
        String isbn,
        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "최소 수량은 1입니다")
        @Max(value = 5, message = "최대 수량은 5입니다")
        Integer quantity
) {
}
