package com.forex.beadando.mnb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MnbExchangeRateClient {
    private static final String SERVICE_NAMESPACE = "http://www.mnb.hu/webservices/";
    private static final String RATE_REQUEST_ACTION = "http://www.mnb.hu/webservices/MNBArfolyamServiceSoap/GetExchangeRates";
    private static final DateTimeFormatter MNB_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> CURRENCIES = List.of(
            "EUR", "USD", "GBP", "CHF", "JPY", "PLN", "CZK", "AUD", "CAD", "CNY", "DKK", "NOK", "SEK"
    );

    private final String endpoint;
    private final HttpClient httpClient;

    public MnbExchangeRateClient(@Value("${mnb.endpoint:http://www.mnb.hu/arfolyamok.asmx}") String endpoint) {
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<String> supportedCurrencies() {
        return CURRENCIES;
    }

    public List<ExchangeRate> getExchangeRates(LocalDate startDate, LocalDate endDate, String currency) {
        String body = "<GetExchangeRates xmlns=\"" + SERVICE_NAMESPACE + "\">"
                + "<startDate>" + MNB_DATE.format(startDate) + "</startDate>"
                + "<endDate>" + MNB_DATE.format(endDate) + "</endDate>"
                + "<currencyNames>" + currency + "</currencyNames>"
                + "</GetExchangeRates>";
        String result = sendRequest(RATE_REQUEST_ACTION, body, "GetExchangeRatesResult");
        return parseRates(result, currency);
    }

    private String sendRequest(String action, String body, String resultElement) {
        String envelope = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + body + "</soap:Body></soap:Envelope>";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"" + action + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MnbServiceException("Az MNB szolgáltatása HTTP " + response.statusCode() + " hibát adott.");
            }
            Document document = parseXml(response.body());
            NodeList faults = document.getElementsByTagNameNS("*", "Fault");
            if (faults.getLength() > 0) {
                throw new MnbServiceException("Az MNB szolgáltatása hibát adott: " + faults.item(0).getTextContent().trim());
            }
            NodeList results = document.getElementsByTagNameNS(SERVICE_NAMESPACE, resultElement);
            if (results.getLength() == 0) {
                throw new MnbServiceException("Az MNB válaszában nem található a kért eredmény.");
            }
            return results.item(0).getTextContent().trim();
        } catch (MnbServiceException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MnbServiceException("Az MNB-lekérdezés megszakadt. Próbáld újra.", exception);
        } catch (Exception exception) {
            throw new MnbServiceException("Nem sikerült elérni az MNB szolgáltatását. Ellenőrizd az internetkapcsolatot, majd próbáld újra.", exception);
        }
    }

    private List<ExchangeRate> parseRates(String xml, String currency) {
        try {
            Document document = parseXml(xml);
            NodeList days = document.getElementsByTagName("Day");
            List<ExchangeRate> rates = new ArrayList<>();
            for (int i = 0; i < days.getLength(); i++) {
                Element day = (Element) days.item(i);
                LocalDate date = LocalDate.parse(day.getAttribute("date"), MNB_DATE);
                NodeList children = day.getElementsByTagName("Rate");
                for (int j = 0; j < children.getLength(); j++) {
                    Element rate = (Element) children.item(j);
                    if (!currency.equalsIgnoreCase(rate.getAttribute("curr"))) {
                        continue;
                    }
                    int unit = Integer.parseInt(rate.getAttribute("unit"));
                    BigDecimal amount = new BigDecimal(rate.getTextContent().trim().replace(',', '.'));
                    BigDecimal normalized = amount.divide(BigDecimal.valueOf(unit), 8, java.math.RoundingMode.HALF_UP);
                    rates.add(new ExchangeRate(date, currency.toUpperCase(Locale.ROOT), unit, normalized));
                }
            }
            return rates;
        } catch (Exception exception) {
            throw new MnbServiceException("Az MNB válaszát nem sikerült feldolgozni.", exception);
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
