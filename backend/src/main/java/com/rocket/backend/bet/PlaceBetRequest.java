package com.rocket.backend.bet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceBetRequest(
        @Min(10) @Max(10_000) long amount,          // bet limits  下注上下限
        @NotBlank @Size(max = 64) String requestId  // client makes a new UUID per click  前端每次点击生成新的 UUID
) {}