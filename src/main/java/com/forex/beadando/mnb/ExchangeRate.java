package com.forex.beadando.mnb;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(LocalDate date, String currency, int sourceUnit, BigDecimal hufPerUnit) {
}
