package com.postfolio.postfolio.stockInvestmentAgents.rag;

import java.util.List;

public class RetrievedContext {

    private final List<String> documents;

    public RetrievedContext(List<String> documents) {
        this.documents = documents;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public String asSingleContextBlock() {
        return String.join("\n\n", documents);
    }
}