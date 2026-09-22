package com.recnaile.indent.dto.response;

import java.util.List;

public record AutoVerifyResponse(
        String indentNumber,
        List<AutoVerifyItemResponse> items) {
}
