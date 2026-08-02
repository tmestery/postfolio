package com.postfolio.postfolio.stockInvestmentAgents;

/**
 * Thrown when a dependency of the agent pipeline (Groq or web research)
 * is missing or unreachable. Controllers translate this into a 503 with a
 * JSON error body.
 */
public class AgentUnavailableException extends RuntimeException {

    public AgentUnavailableException(String message) {
        super(message);
    }

    public AgentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
