package com.trading.bot.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record AnalysisRequestDTO(
        @NotBlank(message = "Elige una criptomoneda")
        @Pattern(regexp = "^[A-Za-z0-9]{1,15}$", message = "El símbolo solo lleva letras y números")
        String symbol,

        @NotNull(message = "Escribe un monto")
        @Positive(message = "El monto debe ser mayor a 0")
        @DecimalMax(value = "1000000000", message = "El monto es demasiado grande")
        Double amountMxn
) {}
