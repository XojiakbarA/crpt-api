package org.example;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private TimeUnit timeUnit;
    private int requestLimit;
    private int requestCount = 0;
    private long lastRequestTime;
    private final String BASE_URL = "https://ismp.crpt.ru/api/v3";
    private String token;
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    public HttpResponse<String> createDocumentForProducedInRF(Document document, String signature) {
        requestCount++;

        checkLimit();

        lastRequestTime = System.currentTimeMillis();

        document.setSignature(signature);

        String json = gson.toJson(document);

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/lk/documents/commissioning/contract/create"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .setHeader("Authorization", "Bearer " + token)
                    .setHeader("Content-Type", "application/json")
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    private void checkLimit() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime <= timeUnit.toMillis(1) && requestCount > requestLimit) {
            throw new RuntimeException("Too many requests");
        }
        if (currentTime - lastRequestTime > timeUnit.toMillis(1)) {
            requestCount = 1;
        }
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    @Data
    public static class Document {
        private DocumentFormat documentFormat;
        private String productDocument;
        private Integer productGroup;
        private DocumentType type;
        private String signature;
    }
    public enum DocumentFormat {
        MANUAL, XML, CSV;
    }
    public enum DocumentType {
        AGGREGATION_DOCUMENT, AGGREGATION_DOCUMENT_CSV, AGGREGATION_DOCUMENT_XML,
        DISAGGREGATION_DOCUMENT, DISAGGREGATION_DOCUMENT_CSV, DISAGGREGATION_DOCUMENT_XML;
    }
}
