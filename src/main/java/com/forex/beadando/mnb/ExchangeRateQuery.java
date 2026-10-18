package com.forex.beadando.mnb;

import java.time.LocalDate;

public class ExchangeRateQuery {
    private String currency = "EUR";
    private LocalDate startDate = LocalDate.now().minusDays(30);
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
