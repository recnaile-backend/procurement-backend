package com.recnaile.masterdata.dto.vendor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVendorRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Email
        @Size(max = 150)
        String email,

        @Size(max = 100)
        String category,

        @Size(max = 50)
        String gstNumber,

        @Size(max = 1000)
        String notes,

        @Size(max = 100)
        String orderSource,

        @Size(max = 500)
        String webUrl,

        Long version
) {
}