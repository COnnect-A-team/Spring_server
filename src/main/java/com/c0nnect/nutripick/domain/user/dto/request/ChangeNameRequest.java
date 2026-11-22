package com.c0nnect.nutripick.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeNameRequest(@NotBlank @Size(max = 100) String name) {}
