package com.postfolio.postfolio.stockInvestmentAgents.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

public class VectorStore {

    private final List<float[]> vectors = new ArrayList<>();
    private final List<String> documents = new ArrayList<>();
    private static final String STORE_PATH = "src/main/java/com/postfolio/postfolio/stockInvestmentAgents/rag/vector_store.json";

    public void add(float[] embedding, String document) {
        vectors.add(embedding);
        documents.add(document);
    }

    public List<String> search(float[] queryEmbedding, int k) {
        PriorityQueue<int[]> pq = new PriorityQueue<>(
                Comparator.comparingInt(a -> a[1])
        );

        for (int i = 0; i < vectors.size(); i++) {
            double similarity = cosineSimilarity(queryEmbedding, vectors.get(i));
            pq.offer(new int[]{i, (int) (similarity * 10000)});
            if (pq.size() > k) pq.poll();
        }

        List<String> results = new ArrayList<>();
        while (!pq.isEmpty()) {
            results.add(documents.get(pq.poll()[0]));
        }

        return results;
    }

    public void saveToDisk() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = new HashMap<>();
            data.put("vectors", vectors);
            data.put("documents", documents);

            mapper.writeValue(new File(STORE_PATH), data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save vector store", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromDisk() {
        try {
            File file = new File(STORE_PATH);
            if (!file.exists()) return;

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data =
                    mapper.readValue(file, Map.class);

            List<List<Double>> loadedVectors =
                    (List<List<Double>>) data.get("vectors");
            List<String> loadedDocs =
                    (List<String>) data.get("documents");

            vectors.clear();
            documents.clear();

            for (List<Double> vec : loadedVectors) {
                float[] f = new float[vec.size()];
                for (int i = 0; i < vec.size(); i++) {
                    f[i] = vec.get(i).floatValue();
                }
                vectors.add(f);
            }

            documents.addAll(loadedDocs);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load vector store", e);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, magA = 0, magB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            magA += a[i] * a[i];
            magB += b[i] * b[i];
        }
        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }
}