package com.forex.beadando.forex;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import static com.oanda.v20.instrument.CandlestickGranularity.*;

@Controller
public class ForexController {

    Context ctx = new Context(Config.URL, Config.TOKEN);

    @GetMapping("/account_info")
    @ResponseBody
    public AccountSummary accountInfo() {
        try {
            AccountSummary summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
            return summary;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/actual_prices")
    public String actualPrices(Model model) {
        model.addAttribute("par", new MessageActPrice());
        return "form_actual_prices";
    }

    @PostMapping("/actual_prices")
    public String actualPrices2(@ModelAttribute MessageActPrice messageActPrice, Model model) {
        String strOut = "";
        List<String> instruments = new ArrayList<>();
        instruments.add(messageActPrice.getInstrument());
        try {
            PricingGetRequest request = new PricingGetRequest(Config.ACCOUNTID, instruments);
            PricingGetResponse resp = ctx.pricing.get(request);
            for (ClientPrice price : resp.getPrices())
                strOut += price + "<br>";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageActPrice.getInstrument());
        model.addAttribute("price", strOut);
        return "result_actual_prices";
    }

    @GetMapping("/hist_prices")
    public String histPrices(Model model) {
        model.addAttribute("param", new MessageHistPrice());
        return "form_hist_prices";
    }

    @PostMapping("/hist_prices")
    public String histPrices2(@ModelAttribute MessageHistPrice messageHistPrice, Model model) {
        String strOut;
        try {
            InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(messageHistPrice.getInstrument()));
            switch (messageHistPrice.getGranularity()) {
                case "M1": request.setGranularity(M1); break;
                case "H1": request.setGranularity(H1); break;
                case "D": request.setGranularity(D); break;
                case "W": request.setGranularity(W); break;
                case "M": request.setGranularity(M); break;
            }
            request.setCount(Long.valueOf(10)); // utolsó 10 árat kérjünk
            InstrumentCandlesResponse resp = ctx.instrument.candles(request);
            strOut = "";
            for (Candlestick candle : resp.getCandles())
                strOut += candle.getTime() + "\t" + candle.getMid().getC() + ";";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("price", strOut);
        return "result_hist_prices";
    }
}
