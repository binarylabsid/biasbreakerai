package com.binarylabsid.biasbreaker_ai.service;

import com.binarylabsid.biasbreaker_ai.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@Service
public class BiasBreakerEngine implements CommandLineRunner {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Autowired
    private ReportCacheService cacheService;

    @Value("${sectors.api.key}")
    private String SECTORS_API_KEY;

    @Value("${telegram.bot.token}")
    private String TELEGRAM_BOT_TOKEN;

    @Value("${telegram.chat.id}")
    private String TELEGRAM_CHAT_ID;

    private final String BASE_URL = "https://api.sectors.app/v2";

    public BiasBreakerEngine() {
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    /* =====================================================================================
       1. INTERACTIVE TERMINAL UNTUK DEMO HACKATHON
       ===================================================================================== */
    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("\n============================================================");
        System.out.println(" 🚀 SECTORPULSE QUANTITATIVE AI TERMINAL 🚀");
        System.out.println("============================================================");

        while (running) {
            System.out.println("\n[ MAIN CONTROL MENU ]");
            System.out.println("1. 🔴 FETCH REAL SECTORS API (Live Data & Local AI Analysis)");
            System.out.println("2. 🟡 FETCH MOCK DATA (Fast Offline Demo)");
            System.out.println("0. ❌ SHUTDOWN TERMINAL");
            System.out.print("👉 Enter your choice (1/2/0): ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    executeWithCountdown("REAL API (Sectors App)");
                    startInstitutionalWorkflow();
                    break;
                case "2":
                    executeWithCountdown("MOCK MODE (Offline Data)");
                    runInstitutionalRadarMock();
                    break;
                case "0":
                    System.out.println("👋 Shutting down SectorPulse. Goodbye!");
                    running = false;
                    System.exit(0);
                    break;
                default:
                    System.out.println("⚠️ Invalid choice. Please enter 1, 2, or 0.");
            }
        }
    }

    private void executeWithCountdown(String modeName) {
        try {
            System.out.println("\n⚡ Initializing Quantitative Engine for: " + modeName);
            for (int i = 3; i > 0; i--) {
                System.out.print("\rStarting execution in " + i + " seconds...  ");
                Thread.sleep(1000);
            }
            System.out.println("\r🚀 EXECUTING WORKFLOW NOW...                     \n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /* =====================================================================================
       2. FITUR LOGGING API
       ===================================================================================== */
    private void printApiLog(String service, String method, String url, String request, String response) {
        System.out.println("\n[📡 " + service + " API CALL]");
        System.out.println("➡️  " + method + " " + url);

        if (request != null && !request.isEmpty()) {
            System.out.println("📤  PAYLOAD: " + request);
        }
        if (response != null && !response.isEmpty()) {
            System.out.println("📥  RESPONSE: " + response);
        }
        System.out.println("------------------------------------------------------");
    }

    /* =====================================================================================
       3. WORKFLOW: REAL API SECTORS
       ===================================================================================== */
    public void startInstitutionalWorkflow() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", SECTORS_API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<StockWebData> activeStocks = new ArrayList<>();

        try {
            System.out.println("⏳ Fetching Top Movers from Sectors API...");
            String scannerUrl = BASE_URL + "/companies/top-changes/?n_stock=2&periods=1d";

            ResponseEntity<String> rawScannerResponse = restTemplate.exchange(scannerUrl, HttpMethod.GET, entity, String.class);
            printApiLog("SECTORS", "GET", scannerUrl, null, rawScannerResponse.getBody());

            ScannerDTO topMovers = mapper.readValue(rawScannerResponse.getBody(), ScannerDTO.class);
            saveAsMockData("1_scanner_response", topMovers);

            if (topMovers == null) throw new RuntimeException("Failed to fetch Scanner API response");

            if (topMovers.getTop_gainers() != null && topMovers.getTop_gainers().getOneDay() != null) {
                for (ScannerDTO.CompanyChange stock : topMovers.getTop_gainers().getOneDay()) {
                    StockWebData data = fetchStockData(stock.getSymbol(), stock.getName(), stock.getPrice_change(), entity);
                    if (data != null) activeStocks.add(data);
                }
            }

            if (topMovers.getTop_losers() != null && topMovers.getTop_losers().getOneDay() != null) {
                for (ScannerDTO.CompanyChange stock : topMovers.getTop_losers().getOneDay()) {
                    StockWebData data = fetchStockData(stock.getSymbol(), stock.getName(), stock.getPrice_change(), entity);
                    if (data != null) activeStocks.add(data);
                }
            }

            if (!activeStocks.isEmpty()) {
                generateProNewsletter(activeStocks);
            } else {
                System.out.println("⚠️ No valid stock data extracted.");
            }

        } catch (Exception e) {
            System.err.println("❌ [FATAL ERROR] Workflow execution failed: " + e.getMessage());
        }
    }

    private StockWebData fetchStockData(String symbol, String companyName, Double priceChange, HttpEntity<String> entity) {
        try {
            System.out.println("\n🔍 Analyzing fundamentals for: " + symbol);

            String profileUrl = BASE_URL + "/company/report/" + symbol + "/?sections=overview";
            ResponseEntity<String> profileRaw = restTemplate.exchange(profileUrl, HttpMethod.GET, entity, String.class);
            printApiLog("SECTORS", "GET", profileUrl, null, profileRaw.getBody());
            CompanyProfileDTO profile = mapper.readValue(profileRaw.getBody(), CompanyProfileDTO.class);
            saveAsMockData("2_profile_" + symbol, profile);

            String rawSubSector = (profile != null && profile.getOverview() != null) ? profile.getOverview().getSub_sector() : "Unknown Sector";
            String slugSubSector = slugify(rawSubSector);

            String internalUrl = BASE_URL + "/financials/quarterly/" + symbol + "/?n_quarters=4&approx=true";
            ResponseEntity<String> internalRaw = restTemplate.exchange(internalUrl, HttpMethod.GET, entity, String.class);
            printApiLog("SECTORS", "GET", internalUrl, null, internalRaw.getBody());
            QuarterlyFinancialsDTO[] financials = mapper.readValue(internalRaw.getBody(), QuarterlyFinancialsDTO[].class);
            saveAsMockData("3_finance_" + symbol, financials);

            Long lastEarnings = 0L;
            List<Long> chartRev = new ArrayList<>();
            List<Long> chartEarn = new ArrayList<>();

            if (financials != null && financials.length > 0) {
                lastEarnings = financials[0].getEarnings() != null ? financials[0].getEarnings() : 0L;
                for (int i = financials.length - 1; i >= 0; i--) {
                    chartRev.add(financials[i].getRevenue() != null ? financials[i].getRevenue() : 0L);
                    chartEarn.add(financials[i].getEarnings() != null ? financials[i].getEarnings() : 0L);
                }
            }

            String macroUrl = BASE_URL + "/subsector/report/" + slugSubSector + "/?sections=stability,statistics,growth";
            ResponseEntity<String> macroRaw = restTemplate.exchange(macroUrl, HttpMethod.GET, entity, String.class);
            printApiLog("SECTORS", "GET", macroUrl, null, macroRaw.getBody());
            SubsectorReportDTO macroBody = mapper.readValue(macroRaw.getBody(), SubsectorReportDTO.class);
            saveAsMockData("4_macro_" + slugSubSector, macroBody);

            Double medianPe = (macroBody != null && macroBody.getStatistics() != null && macroBody.getStatistics().getFiltered_median_pe() != null) ? macroBody.getStatistics().getFiltered_median_pe() : 0.0;
            Double maxDrawdown = (macroBody != null && macroBody.getStability() != null && macroBody.getStability().getWeighted_max_drawdown() != null) ? macroBody.getStability().getWeighted_max_drawdown() : 0.0;
            Double forecastEps = (macroBody != null && macroBody.getGrowth() != null && macroBody.getGrowth().getGrowthForecasts() != null && macroBody.getGrowth().getGrowthForecasts().containsKey("2026")) ? macroBody.getGrowth().getGrowthForecasts().get("2026").getEpsGrowth() : 0.0;

            String cleanSymbol = symbol.replace(".JK", "");

            StockWebData stockData = new StockWebData();
            stockData.setSymbol(cleanSymbol);
            stockData.setCompanyName(companyName != null ? companyName : cleanSymbol);
            stockData.setSector(rawSubSector);
            stockData.setPriceChange(String.format("%.2f%%", (priceChange != null ? priceChange : 0) * 100));

            stockData.setLastEarnings(String.format(java.util.Locale.forLanguageTag("id-ID"), "Rp %,d", lastEarnings));

            stockData.setPeRatio(String.format("%.2fx", medianPe));
            stockData.setMaxDrawdown(String.format("%.2f%%", maxDrawdown * 100));
            stockData.setForecastGrowth(String.format("%.2f%%", forecastEps * 100));
            stockData.setChartRevenue(chartRev);
            stockData.setChartEarnings(chartEarn);

            System.out.println("✅ Data extraction complete for: " + cleanSymbol);
            return stockData;

        } catch (Exception e) {
            System.err.println("❌ Failed to process data for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    /* =====================================================================================
       4. WORKFLOW: MOCK MODE OFFLINE (DYNAMIC FROM JSON FILES)
       ===================================================================================== */
    public void runInstitutionalRadarMock() {
        List<StockWebData> activeStocks = new ArrayList<>();
        System.out.println("⏳ Loading Offline Mock Data from local storage...");

        try {
            String scannerJson = new String(Files.readAllBytes(Paths.get("mock-data/1_scanner_response.json")));
            ScannerDTO topMovers = mapper.readValue(scannerJson, ScannerDTO.class);

            List<ScannerDTO.CompanyChange> stocksToProcess = new ArrayList<>();

            if (topMovers != null) {
                if (topMovers.getTop_gainers() != null && topMovers.getTop_gainers().getOneDay() != null) {
                    stocksToProcess.addAll(topMovers.getTop_gainers().getOneDay());
                }
                if (topMovers.getTop_losers() != null && topMovers.getTop_losers().getOneDay() != null) {
                    stocksToProcess.addAll(topMovers.getTop_losers().getOneDay());
                }
            }

            if (stocksToProcess.isEmpty()) {
                System.out.println("⚠️ Mock Scanner data is empty. Ensure '1_scanner_response.json' exists and contains data.");
                return;
            }

            for (ScannerDTO.CompanyChange stock : stocksToProcess) {
                String symbol = stock.getSymbol();
                String companyName = stock.getName();
                Double priceChange = stock.getPrice_change();
                String cleanSymbol = symbol.replace(".JK", "");

                try {
                    String profileJson = new String(Files.readAllBytes(Paths.get("mock-data/2_profile_" + symbol + ".json")));
                    CompanyProfileDTO profile = mapper.readValue(profileJson, CompanyProfileDTO.class);
                    String rawSubSector = (profile != null && profile.getOverview() != null) ? profile.getOverview().getSub_sector() : "Unknown Sector";
                    String slugSubSector = slugify(rawSubSector);

                    String financeJson = new String(Files.readAllBytes(Paths.get("mock-data/3_finance_" + symbol + ".json")));
                    QuarterlyFinancialsDTO[] financials = mapper.readValue(financeJson, QuarterlyFinancialsDTO[].class);
                    Long earnings = (financials != null && financials.length > 0 && financials[0].getEarnings() != null) ? financials[0].getEarnings() : 0L;

                    List<Long> chartRev = new ArrayList<>();
                    List<Long> chartEarn = new ArrayList<>();
                    if (financials != null && financials.length > 0) {
                        for (int i = financials.length - 1; i >= 0; i--) {
                            chartRev.add(financials[i].getRevenue() != null ? financials[i].getRevenue() : 0L);
                            chartEarn.add(financials[i].getEarnings() != null ? financials[i].getEarnings() : 0L);
                        }
                    } else {
                        chartRev = Arrays.asList(120000000L, 150000000L, 140000000L, 180000000L);
                        chartEarn = Arrays.asList(30000000L, 45000000L, 35000000L, 60000000L);
                    }

                    String macroJson = new String(Files.readAllBytes(Paths.get("mock-data/4_macro_" + slugSubSector + ".json")));
                    SubsectorReportDTO macro = mapper.readValue(macroJson, SubsectorReportDTO.class);
                    Double medianPe = (macro.getStatistics() != null && macro.getStatistics().getFiltered_median_pe() != null) ? macro.getStatistics().getFiltered_median_pe() : 0.0;
                    Double maxDrawdown = (macro.getStability() != null && macro.getStability().getWeighted_max_drawdown() != null) ? macro.getStability().getWeighted_max_drawdown() : 0.0;
                    Double forecastEps = (macro.getGrowth() != null && macro.getGrowth().getGrowthForecasts() != null && macro.getGrowth().getGrowthForecasts().containsKey("2026")) ? macro.getGrowth().getGrowthForecasts().get("2026").getEpsGrowth() : 0.0;

                    StockWebData stockData = new StockWebData();
                    stockData.setSymbol(cleanSymbol);
                    stockData.setCompanyName(companyName != null ? companyName : cleanSymbol);
                    stockData.setSector(rawSubSector);
                    stockData.setPriceChange(String.format("%.2f%%", (priceChange != null ? priceChange : 0) * 100));

                    stockData.setLastEarnings(String.format(java.util.Locale.forLanguageTag("id-ID"), "Rp %,d", earnings));

                    stockData.setPeRatio(String.format("%.2fx", medianPe));
                    stockData.setMaxDrawdown(String.format("%.2f%%", maxDrawdown * 100));
                    stockData.setForecastGrowth(String.format("%.2f%%", forecastEps * 100));
                    stockData.setChartRevenue(chartRev);
                    stockData.setChartEarnings(chartEarn);

                    activeStocks.add(stockData);
                    System.out.println("✅ Mock data fully loaded for: " + cleanSymbol);

                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to load mock details for " + symbol + " (File might be missing): " + ex.getMessage());
                }
            }

            if (!activeStocks.isEmpty()) {
                generateProNewsletter(activeStocks);
            } else {
                System.out.println("⚠️ No valid stock data extracted from Mock files.");
            }

        } catch (Exception e) {
            System.err.println("❌ Fatal error while reading Scanner Mock: " + e.getMessage());
        }
    }

    /* =====================================================================================
       5. LOCAL OLLAMA AI GENERATION & TELEGRAM DELIVERY (WITH ENTERPRISE FALLBACK)
       ===================================================================================== */
    private void generateProNewsletter(List<StockWebData> activeStocks) {
        StringBuilder marketDataPayload = new StringBuilder();
        for (StockWebData stock : activeStocks) {
            marketDataPayload.append(String.format("Stock: %s (%s)\nSector: %s\nMovement: %s\nNet Earnings: %s\nSector P/E: %s\nMax Drawdown: %s\nGrowth 2026: %s\n\n",
                    stock.getCompanyName(), stock.getSymbol(), stock.getSector(), stock.getPriceChange(),
                    stock.getLastEarnings(), stock.getPeRatio(), stock.getMaxDrawdown(), stock.getForecastGrowth()));
        }

        String userPrompt = "Stock Data:\n" + marketDataPayload +
                "\n\nGenerate the JSON exactly according to the SYSTEM PROMPT schema. ONLY valid JSON. " +
                "CRITICAL INSTRUCTION: Pay extremely close attention to NEGATIVE signs (-) in Net Earnings and Movement. " +
                "DO NOT copy-paste analysis between stocks. Treat each stock uniquely. If earnings are negative, the bull_read must NOT say it has strong earnings!";

        boolean aiSuccess = false;
        AiNewsletterResponse aiData = null;

        try {
            String llmApiUrl = "http://localhost:11434/v1/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            LlmDTO.Request requestBody = new LlmDTO.Request();
            requestBody.setModel("sectorpulse");
            requestBody.setTemperature(0.0);
            requestBody.setMessages(List.of(new LlmDTO.Message("user", userPrompt)));

            String payloadJson = mapper.writeValueAsString(requestBody);

            System.out.print("\n");
            long startTime = System.currentTimeMillis();
            Thread timerThread = new Thread(() -> {
                try {
                    int seconds = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(1000);
                        seconds++;
                        System.out.print("\r🧠 Local AI (Ollama) is generating analysis... " + seconds + "s");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            timerThread.start();

            ResponseEntity<String> rawLlmResponse = restTemplate.exchange(llmApiUrl, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);
            timerThread.interrupt();

            long endTime = System.currentTimeMillis();
            double totalTimeInSeconds = (endTime - startTime) / 1000.0;
            System.out.println("\r✅ [SUCCESS] AI Analysis completed in " + String.format("%.1f", totalTimeInSeconds) + " seconds!                  ");

            printApiLog("LOCAL OLLAMA", "POST", llmApiUrl, null, rawLlmResponse.getBody());

            LlmDTO.Response responseDto = mapper.readValue(rawLlmResponse.getBody(), LlmDTO.Response.class);
            String cleanJson = responseDto.getChoices().get(0).getMessage().getContent();

            if (cleanJson.contains("```json")) cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7, cleanJson.lastIndexOf("```"));
            else if (cleanJson.contains("```")) cleanJson = cleanJson.substring(cleanJson.indexOf("```") + 3, cleanJson.lastIndexOf("```"));
            cleanJson = cleanJson.trim();

            aiData = mapper.readValue(cleanJson, AiNewsletterResponse.class);
            aiSuccess = true;

        } catch (Exception e) {
            System.err.println("\n⚠️ [WARNING] Local AI Failed or Offline: " + e.getMessage());
            System.out.println("🔄 Engaging Deterministic Quantitative Auto-Fallback Engine...");
        }

        if (!aiSuccess || aiData == null) {
            aiData = new AiNewsletterResponse();
            aiData.setDaily_insights("Market movements today reflect aggressive capital rotation. Institutional investors are actively avoiding companies with fragile fundamentals while accumulating shares in fundamentally sound businesses.");

            List<AiNewsletterResponse.AiStock> aiStocks = new ArrayList<>();
            for (StockWebData stock : activeStocks) {
                AiNewsletterResponse.AiStock aiStock = new AiNewsletterResponse.AiStock();
                aiStock.setSymbol(stock.getSymbol());
                boolean isLoss = stock.getLastEarnings().contains("-");
                if (isLoss) {
                    aiStock.setBull_read("No positive catalyst identified. The company is currently posting severe net losses of " + stock.getLastEarnings() + ".");
                    aiStock.setBear_read("Extreme high risk! The " + stock.getSector() + " sector has a brutal historical Max Drawdown of " + stock.getMaxDrawdown() + ".");
                    aiStock.setVerdict("AVOID");
                    aiStock.setVerdict_reason("Negative earnings coupled with extreme sector volatility make this equity uninvestable right now.");
                    aiStock.setBeginner_insight_pe("Buying this stock is like renting an empty store; it brings no income but you still pay the electricity bill.");
                    aiStock.setBeginner_insight_risk("A drawdown of " + stock.getMaxDrawdown() + " means if you invest $1,000, you could lose most of it in a panic sell-off.");
                } else {
                    aiStock.setBull_read("Solid financials. The company successfully generated net earnings of " + stock.getLastEarnings() + ", proving resilience.");
                    aiStock.setBear_read("Remain cautious, historical data shows this sector can experience a Max Drawdown up to " + stock.getMaxDrawdown() + " during bear markets.");
                    aiStock.setVerdict("BUY");
                    aiStock.setVerdict_reason("Healthy profitability and a stable margin of safety justify accumulation at current levels.");
                    aiStock.setBeginner_insight_pe("A P/E of " + stock.getPeRatio() + " is like knowing exactly how many years it takes for your business to pay for itself.");
                    aiStock.setBeginner_insight_risk("A " + stock.getMaxDrawdown() + " drop is like a massive seasonal discount. If you have cash, it's an opportunity.");
                }
                aiStocks.add(aiStock);
            }
            aiData.setStocks(aiStocks);
            aiData.setStrategy_tactics(Arrays.asList("Avoid speculative equities with negative cash flow.", "Maintain 30% cash reserves for tactical buying during drawdowns."));

            AiNewsletterResponse.AiGlossary fallbackGlossary = new AiNewsletterResponse.AiGlossary();
            fallbackGlossary.setTerm("Max Drawdown");
            fallbackGlossary.setDefinition("The maximum observed loss from a peak to a trough of a portfolio.");
            aiData.setGlossary(List.of(fallbackGlossary));
        }

        StringBuilder introHtml = new StringBuilder();
        introHtml.append("<b>📊 SECTORPULSE EXECUTIVE INSIGHTS</b>\n");
        introHtml.append(escapeHtml(aiData.getDaily_insights())).append("\n\n");
        introHtml.append("<b>🚀 TOP MOVERS & QUANT ANALYSIS</b>");
        sendToTelegram(introHtml.toString());

        for (StockWebData stock : activeStocks) {
            AiNewsletterResponse.AiStock aiStock = aiData.getStocks().stream().filter(s -> s.getSymbol().equals(stock.getSymbol())).findFirst().orElse(null);
            if (aiStock != null) {
                stock.setVerdictAI(aiStock.getVerdict());
                stock.setVerdictReason(aiStock.getVerdict_reason());
                stock.setBullRead(aiStock.getBull_read());
                stock.setBearRead(aiStock.getBear_read());
                stock.setBeginnerInsightPe(aiStock.getBeginner_insight_pe());
                stock.setBeginnerInsightRisk(aiStock.getBeginner_insight_risk());

                cacheService.saveReport(stock.getSymbol(), stock);

                StringBuilder stockHtml = new StringBuilder();
                stockHtml.append(String.format("<b>%s (%s)</b> - <i>%s</i>\n", escapeHtml(stock.getCompanyName()), escapeHtml(stock.getSymbol()), escapeHtml(stock.getSector())));
                stockHtml.append(String.format("📈/📉 <b>Movement:</b> %s\n", escapeHtml(stock.getPriceChange())));
                stockHtml.append(String.format("💰 <b>Latest Earnings:</b> %s\n", escapeHtml(stock.getLastEarnings())));

                stockHtml.append("🟢 <b>The Bull Read:</b>\n").append(escapeHtml(stock.getBullRead())).append("\n");
                stockHtml.append("🔴 <b>The Bear Read:</b>\n").append(escapeHtml(stock.getBearRead())).append("\n\n");

                stockHtml.append("💡 <b>Beginner Insight (Valuation):</b>\n").append(escapeHtml(stock.getBeginnerInsightPe())).append("\n");
                stockHtml.append("⚠️ <b>Beginner Insight (Risk):</b>\n").append(escapeHtml(stock.getBeginnerInsightRisk())).append("\n\n");

                stockHtml.append(String.format("🎯 <b>VERDICT:</b> <b>%s</b> - %s\n\n", escapeHtml(stock.getVerdictAI()), escapeHtml(stock.getVerdictReason())));

                stockHtml.append(String.format("🔗 <a href=\"https://retying-anywhere-diffuser.ngrok-free.dev/report/%s\">Open SectorPulse Web Report</a>\n", stock.getSymbol()));
                stockHtml.append(String.format("🔗 <a href=\"https://sectors.app/idx/%s\">Verify Data on Sectors</a>", stock.getSymbol()));

                sendToTelegram(stockHtml.toString());

                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        StringBuilder outroHtml = new StringBuilder();
        outroHtml.append("<b>💡 STRATEGY & TACTICS</b>\n");
        for (String tactic : aiData.getStrategy_tactics()) {
            outroHtml.append("• ").append(escapeHtml(tactic)).append("\n");
        }

        outroHtml.append("\n<b>📚 INVESTOR GLOSSARY</b>\n");
        for (AiNewsletterResponse.AiGlossary gloss : aiData.getGlossary()) {
            outroHtml.append("• <b>").append(escapeHtml(gloss.getTerm())).append(":</b> ").append(escapeHtml(gloss.getDefinition())).append("\n");
        }
        sendToTelegram(outroHtml.toString());
    }

    private void sendToTelegram(String message) {
        String telegramUrl = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            TelegramRequest requestBody = new TelegramRequest(TELEGRAM_CHAT_ID, message);
            restTemplate.exchange(telegramUrl, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);
            System.out.println("✅ [SUCCESS] Sent message part to Telegram!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send Telegram message. Error detail: " + e.getMessage());
        }
    }

    private void saveAsMockData(String fileName, Object dtoObject) {
        try {
            File directory = new File("mock-data");
            if (!directory.exists()) directory.mkdir();
            File file = new File(directory, fileName + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, dtoObject);
        } catch (Exception e) {}
    }

    private String slugify(String text) {
        if (text == null) return "";
        return text.toLowerCase().replace(",", "").replace(" & ", "-").replace(" ", "-").replaceAll("-+", "-");
    }

    public static class TelegramRequest {
        public String chat_id;
        public String text;
        public String parse_mode = "HTML";
        public boolean disable_web_page_preview = true;

        public TelegramRequest(String chat_id, String text) {
            this.chat_id = chat_id;
            this.text = text;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}