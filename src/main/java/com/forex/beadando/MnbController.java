package com.forex.beadando;

import com.forex.beadando.mnb.ExchangeRate;
import com.forex.beadando.mnb.ExchangeRateQuery;
import com.forex.beadando.mnb.MnbExchangeRateClient;
import com.forex.beadando.mnb.MnbServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class MnbController {
    private final MnbExchangeRateClient exchangeRateClient;
    private final String team;

    public MnbController(MnbExchangeRateClient exchangeRateClient,
                         @Value("${app.team:Projektcsapat}") String team) {
        this.exchangeRateClient = exchangeRateClient;
        this.team = team;
    }

    @ModelAttribute("team")
    public String team() {
        return team;
    }

    @GetMapping("/mnb")
    public String showQuery(Model model) {
        model.addAttribute("query", new ExchangeRateQuery());
        model.addAttribute("currencies", exchangeRateClient.supportedCurrencies());
        model.addAttribute("searched", false);
        return "mnb";
    }

    @PostMapping("/mnb")
    public String submitQuery(@ModelAttribute("query") ExchangeRateQuery query, Model model) {
        model.addAttribute("currencies", exchangeRateClient.supportedCurrencies());
        String validationError = validate(query);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            model.addAttribute("searched", false);
            return "mnb";
        }

        try {
            List<ExchangeRate> rates = exchangeRateClient.getExchangeRates(
                    query.getStartDate(), query.getEndDate(), query.getCurrency());
            model.addAttribute("rates", rates);
            model.addAttribute("searched", true);
        } catch (MnbServiceException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("searched", false);
        }
        return "mnb";
    }

    private String validate(ExchangeRateQuery query) {
        if (query.getCurrency() == null
                || !exchangeRateClient.supportedCurrencies().contains(query.getCurrency())) {
            return "Válassz egy támogatott devizát.";
        }
        LocalDate start = query.getStartDate();
        LocalDate end = query.getEndDate();
        if (start == null || end == null) {
            return "Add meg a kezdő és a záró dátumot.";
        }
        if (start.isAfter(end)) {
            return "A kezdő dátum nem lehet későbbi a záró dátumnál.";
        }
        if (end.isAfter(LocalDate.now())) {
            return "A záró dátum nem lehet jövőbeli.";
        }
        return null;
    }
}
