package com.bobocode.controller;

import com.bobocode.service.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/pictures")
@RequiredArgsConstructor
public class LargestPictureController {
    private final PictureService pictureService;
    
    @GetMapping(value = "/{sol}/largest", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<byte[]> getLargestPicture(@PathVariable int sol) {
        return pictureService.getLargestPicture(sol)
                .flatMap(url -> WebClient.create(url)
                        .mutate()
                        .codecs(config -> config.defaultCodecs().maxInMemorySize(10_000_000))
                        .build()
                        .get()
                        .exchangeToMono(resp -> resp.bodyToMono(byte[].class))
                );
    }
}
