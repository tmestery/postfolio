package com.postfolio.postfolio.models.symbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a demo-friendly US equity / ETF universe when {@code stock_symbol} is empty.
 * For a larger reload against Postgres, see {@code scripts/seed-stock-symbols.sql}.
 */
@Component
public class StockSymbolSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockSymbolSeeder.class);

    private final StockSymbolRepository repository;

    public StockSymbolSeeder(StockSymbolRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        List<StockSymbol> seed = demoUniverse();
        repository.saveAll(seed);
        log.info("Seeded {} stock symbols into stock_symbol", seed.size());
    }

    static List<StockSymbol> demoUniverse() {
        String[][] rows = {
                {"AAPL", "Apple Inc."},
                {"MSFT", "Microsoft Corporation"},
                {"GOOGL", "Alphabet Inc. Class A"},
                {"GOOG", "Alphabet Inc. Class C"},
                {"AMZN", "Amazon.com Inc."},
                {"META", "Meta Platforms Inc."},
                {"NVDA", "NVIDIA Corporation"},
                {"TSLA", "Tesla Inc."},
                {"BRK.B", "Berkshire Hathaway Inc. Class B"},
                {"JPM", "JPMorgan Chase & Co."},
                {"V", "Visa Inc."},
                {"MA", "Mastercard Incorporated"},
                {"UNH", "UnitedHealth Group Incorporated"},
                {"XOM", "Exxon Mobil Corporation"},
                {"JNJ", "Johnson & Johnson"},
                {"WMT", "Walmart Inc."},
                {"PG", "Procter & Gamble Company"},
                {"HD", "The Home Depot Inc."},
                {"CVX", "Chevron Corporation"},
                {"MRK", "Merck & Co. Inc."},
                {"ABBV", "AbbVie Inc."},
                {"PEP", "PepsiCo Inc."},
                {"KO", "The Coca-Cola Company"},
                {"COST", "Costco Wholesale Corporation"},
                {"AVGO", "Broadcom Inc."},
                {"LLY", "Eli Lilly and Company"},
                {"BAC", "Bank of America Corporation"},
                {"CRM", "Salesforce Inc."},
                {"AMD", "Advanced Micro Devices Inc."},
                {"NFLX", "Netflix Inc."},
                {"ADBE", "Adobe Inc."},
                {"TMO", "Thermo Fisher Scientific Inc."},
                {"CSCO", "Cisco Systems Inc."},
                {"ORCL", "Oracle Corporation"},
                {"ACN", "Accenture plc"},
                {"MCD", "McDonald's Corporation"},
                {"INTC", "Intel Corporation"},
                {"IBM", "International Business Machines"},
                {"QCOM", "QUALCOMM Incorporated"},
                {"TXN", "Texas Instruments Incorporated"},
                {"AMAT", "Applied Materials Inc."},
                {"INTU", "Intuit Inc."},
                {"AMGN", "Amgen Inc."},
                {"ISRG", "Intuitive Surgical Inc."},
                {"BKNG", "Booking Holdings Inc."},
                {"NOW", "ServiceNow Inc."},
                {"UBER", "Uber Technologies Inc."},
                {"SBUX", "Starbucks Corporation"},
                {"NKE", "NIKE Inc."},
                {"DIS", "The Walt Disney Company"},
                {"BA", "The Boeing Company"},
                {"CAT", "Caterpillar Inc."},
                {"GE", "GE Aerospace"},
                {"GS", "The Goldman Sachs Group Inc."},
                {"MS", "Morgan Stanley"},
                {"BLK", "BlackRock Inc."},
                {"AXP", "American Express Company"},
                {"SCHW", "Charles Schwab Corporation"},
                {"PYPL", "PayPal Holdings Inc."},
                {"SQ", "Block Inc."},
                {"SHOP", "Shopify Inc."},
                {"SPOT", "Spotify Technology S.A."},
                {"SNAP", "Snap Inc."},
                {"PINS", "Pinterest Inc."},
                {"COIN", "Coinbase Global Inc."},
                {"PLTR", "Palantir Technologies Inc."},
                {"SNOW", "Snowflake Inc."},
                {"CRWD", "CrowdStrike Holdings Inc."},
                {"PANW", "Palo Alto Networks Inc."},
                {"DDOG", "Datadog Inc."},
                {"ZM", "Zoom Communications Inc."},
                {"RBLX", "Roblox Corporation"},
                {"ABNB", "Airbnb Inc."},
                {"DAL", "Delta Air Lines Inc."},
                {"UAL", "United Airlines Holdings Inc."},
                {"F", "Ford Motor Company"},
                {"GM", "General Motors Company"},
                {"RIVN", "Rivian Automotive Inc."},
                {"LCID", "Lucid Group Inc."},
                {"NEE", "NextEra Energy Inc."},
                {"SO", "The Southern Company"},
                {"DUK", "Duke Energy Corporation"},
                {"SPY", "SPDR S&P 500 ETF Trust"},
                {"QQQ", "Invesco QQQ Trust"},
                {"IWM", "iShares Russell 2000 ETF"},
                {"DIA", "SPDR Dow Jones Industrial Average ETF"},
                {"VTI", "Vanguard Total Stock Market ETF"},
                {"VOO", "Vanguard S&P 500 ETF"},
                {"ARKK", "ARK Innovation ETF"},
                {"XLF", "Financial Select Sector SPDR Fund"},
                {"XLK", "Technology Select Sector SPDR Fund"},
                {"XLE", "Energy Select Sector SPDR Fund"},
                {"GLD", "SPDR Gold Shares"},
                {"SLV", "iShares Silver Trust"},
                {"TLT", "iShares 20+ Year Treasury Bond ETF"},
                {"TQQQ", "ProShares UltraPro QQQ"},
                {"SOXX", "iShares Semiconductor ETF"},
                {"SMH", "VanEck Semiconductor ETF"},
        };
        List<StockSymbol> out = new ArrayList<>(rows.length);
        for (String[] row : rows) {
            out.add(new StockSymbol(row[0], row[1]));
        }
        return out;
    }
}
