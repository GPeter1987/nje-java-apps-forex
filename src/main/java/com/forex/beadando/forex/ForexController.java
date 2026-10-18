package com.forex.beadando.forex;

import com.oanda.v20.Context;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeSpecifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.oanda.v20.instrument.CandlestickGranularity.*;

@Controller
public class ForexController {

    Context ctx = new Context(Config.URL, Config.TOKEN);

    // A lenyíló listák instrumentumai (Forex-AktÁr, Forex-HistÁr, Forex-Nyit)
    @ModelAttribute("instruments")
    public List<String> instruments() {
        return List.of("EUR_USD", "GBP_USD", "USD_JPY", "USD_CHF", "AUD_USD", "USD_CAD", "NZD_USD", "EUR_GBP", "EUR_JPY");
    }

    // A Forex-HistÁr lenyíló listájának értékei: ugyanazok, amelyeket a histPrices2 switch-e kezel
    @ModelAttribute("granularities")
    public Map<String, String> granularities() {
        Map<String, String> g = new LinkedHashMap<>();
        g.put("M1", "M1 – 1 perces");
        g.put("H1", "H1 – 1 órás");
        g.put("D", "D – napi");
        g.put("W", "W – heti");
        g.put("M", "M – havi");
        return g;
    }

    @GetMapping("/account_info")
    public String accountInfo(Model model) {
        try {
            AccountSummary summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
            model.addAttribute("summary", summary);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "A számlainformációk lekérdezése nem sikerült: " + errorText(e));
        }
        return "account_info";
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
            // A sablon táblázatához az árobjektumokat is átadjuk (bid, ask, idő külön-külön)
            model.addAttribute("prices", resp.getPrices());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Az aktuális ár lekérdezése nem sikerült: " + errorText(e));
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
        String strOut = "";
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
            // A sablon táblázatához a gyertyákat is átadjuk (idő, nyitó, max, min, záró ár)
            model.addAttribute("candles", resp.getCandles());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "A historikus árak lekérdezése nem sikerült: " + errorText(e));
        }
        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("price", strOut);
        return "result_hist_prices";
    }

    @GetMapping("/open_position")
    public String openPosition(Model model) {
        model.addAttribute("param", new MessageOpenPosition());
        return "form_open_position";
    }

    @PostMapping("/open_position")
    public String openPosition2(@ModelAttribute MessageOpenPosition messageOpenPosition, Model model) {
        String strOut = "";
        if (messageOpenPosition.getUnits() == 0) {
            // 0 mennyiséggel nincs mit megnyitni, ilyenkor nem is küldünk megbízást
            model.addAttribute("error", "A mennyiség nem lehet 0. Pozitív szám: Long, negatív szám: Short pozíció.");
        } else {
            try {
                InstrumentName instrument = new InstrumentName(messageOpenPosition.getInstrument());
                OrderCreateRequest request = new OrderCreateRequest(Config.ACCOUNTID);
                MarketOrderRequest marketorderrequest = new MarketOrderRequest();
                marketorderrequest.setInstrument(instrument);
                // Ha pozitív, akkor LONG, ha negatív, akkor SHORT:
                marketorderrequest.setUnits(messageOpenPosition.getUnits());
                request.setOrder(marketorderrequest);
                OrderCreateResponse response = ctx.order.create(request);
                if (response.getOrderFillTransaction() != null) {
                    strOut = "tradeId: " + response.getOrderFillTransaction().getId();
                    model.addAttribute("fill", response.getOrderFillTransaction());
                } else if (response.getOrderCancelTransaction() != null) {
                    // Pl. zárt piacnál az Oanda nem teljesíti, hanem visszavonja a megbízást
                    model.addAttribute("error", "Az Oanda visszavonta a megbízást: "
                            + response.getOrderCancelTransaction().getReason()
                            + ". A devizapiac hétfőtől péntekig van nyitva.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("error", "A pozíció megnyitása nem sikerült: " + errorText(e));
            }
        }
        model.addAttribute("instr", messageOpenPosition.getInstrument());
        model.addAttribute("units", messageOpenPosition.getUnits());
        model.addAttribute("id", strOut);
        return "result_open_position";
    }

    @GetMapping("/positions")
    public String positions(Model model) {
        String strOut = "Open positions:<br>";
        try {
            List<Trade> trades = ctx.trade.listOpen(Config.ACCOUNTID).getTrades();
            for (Trade trade : trades)
                strOut += trade.getId() + ", " + trade.getInstrument() + ", " + trade.getOpenTime() + ", "
                        + trade.getCurrentUnits() + ", " + trade.getPrice() + ", " + trade.getUnrealizedPL() + "<br>";
            // A feladat táblázatot kér: a sablon a trade-ek listájából építi fel
            model.addAttribute("trades", trades);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "A nyitott pozíciók lekérdezése nem sikerült: " + errorText(e));
        }
        model.addAttribute("positions", strOut);
        return "positions";
    }

    @GetMapping("/close_position")
    public String closePosition(Model model) {
        model.addAttribute("param", new MessageClosePosition());
        return "form_close_position";
    }

    @PostMapping("/close_position")
    public String closePosition2(@ModelAttribute MessageClosePosition messageClosePosition, Model model) {
        String tradeId = messageClosePosition.getTradeId() + "";
        String strOut = "Closed tradeId= " + tradeId;
        try {
            ctx.trade.close(new TradeCloseRequest(Config.ACCOUNTID, new TradeSpecifier(tradeId)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("tradeId", strOut);
        return "result_close_position";
    }

    // Az Oanda hibaválaszainál (RequestException) a szöveg az errorMessage mezőben van, nem a getMessage()-ben
    private String errorText(Exception e) {
        if (e instanceof RequestException re && re.getErrorMessage() != null)
            return re.getErrorMessage();
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
