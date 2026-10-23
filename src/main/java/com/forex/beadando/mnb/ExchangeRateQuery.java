package com.forex.beadando.mnb;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ExchangeRateQuery {
    private String currency = "EUR";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now().minusDays(30);
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate = LocalDate.now();

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
