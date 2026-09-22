package com.recnaile.indent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectIndentRequest(
        @NotBlank String reason
) {}
