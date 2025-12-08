package com.postfolio.postfolio.stockInvestmentAgents.managerAgents;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class DataScraper {

    public static class NewsItem {
        public String title;
        public String summary;
        public String url;

        public NewsItem(String title, String summary, String url) {
            this.title = title;
            this.summary = summary;
            this.url = url;
        }
    }

    public static List<NewsItem> fetchLatestMarketNews() {
        List<NewsItem> news = new ArrayList<>();

        try {
            String url = "https://www.reuters.com/markets/";
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            // Each article card
            Elements articles = doc.select("article");

            for (Element article : articles) {
                Element titleEl = article.selectFirst("h3");
                Element summaryEl = article.selectFirst("p");
                Element linkEl = article.selectFirst("a");

                if (titleEl == null || linkEl == null) continue;

                String title = titleEl.text();
                String summary = summaryEl != null ? summaryEl.text() : "";
                String link = linkEl.absUrl("href");

                news.add(new NewsItem(title, summary, link));

                // stop after reasonable amount
                if (news.size() >= 15) break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return news;
    }

    public static void main(String[] args) {
        List<NewsItem> news = fetchLatestMarketNews();

        for (NewsItem item : news) {
            System.out.println("TITLE: " + item.title);
            System.out.println("SUMMARY: " + item.summary);
            System.out.println("URL: " + item.url);
            System.out.println("-----");
        }
    }
}