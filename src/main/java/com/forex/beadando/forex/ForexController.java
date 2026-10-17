package com.forex.beadando.forex;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
