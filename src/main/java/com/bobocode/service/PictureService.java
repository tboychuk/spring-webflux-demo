package com.bobocode.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class PictureService {
    @Value("${nasa.api.url}")
    private String nasaBaseApiUrl;

    @Value("${nasa.api.key}")
    private String nasaApiKey;

    public Mono<String> getLargestPicture(int sol) {
        return WebClient.create(nasaBaseApiUrl)
                .get()
                .uri(builder -> builder
                        .queryParam("api_key", nasaApiKey)
                        .queryParam("sol", sol)
                        .build()
                )
                .exchangeToMono(resp -> resp.bodyToMono(JsonNode.class))
                .map(this::fetchPictureUrls)
                .flatMapMany(Flux::fromIterable)
                .flatMap(pictureUrl -> WebClient.create(pictureUrl)
                        .head()
                        .exchangeToMono(ClientResponse::toBodilessEntity)
                        .map(this::fetchRedirectUrl)
                        .flatMap(redirectedUrl -> WebClient.create(redirectedUrl)
                                .head()
                                .exchangeToMono(ClientResponse::toBodilessEntity)
                                .map(this::fetchPictureSize)
                                .map(size -> new Picture(redirectedUrl, size))
                        )
                )
                .reduce((p1, p2) -> p1.size > p2.size ? p1 : p2)
                .map(Picture::url);
    }

    private List<String> fetchPictureUrls(JsonNode jsonNode) {
        return StreamSupport.stream(jsonNode.get("photos").spliterator(), false)
                .map(p -> p.get("img_src"))
                .map(JsonNode::asText)
                .toList();
    }

    private String fetchRedirectUrl(ResponseEntity<?> responseEntity) {
        var headers= responseEntity.getHeaders();
        var location = headers.getLocation();
        return location.toString();
    }

    private long fetchPictureSize(ResponseEntity<?> responseEntity) {
        var headers= responseEntity.getHeaders();
        return headers.getContentLength();
    }

    record Picture(String url, Long size) {
    }

}
