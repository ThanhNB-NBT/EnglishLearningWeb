package com.thanhnb.englishlearning.dto.user.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 100)
    private String fullName;
}
