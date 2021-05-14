package com.sokfa.reactivo.reactivohastaelfinal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.reactive.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RestController
public class FlujoBase {

    @Value("${app.url:http://example.com}")
    private String url = "http://example.com";

    private static Logger log = LoggerFactory.getLogger(FlujoBase.class);
    private RestTemplate restTemplate = new RestTemplate();
    private WebClient client = new WebClient(new ReactorClientHttpConnector());
    private Scheduler scheduler = Schedulers.elastic();

    @RequestMapping("/parallel")
    public Mono<Result> parallel() {
        log.info("Handling /parallel");
        return Flux.range(1, 10)
                .log()
                .flatMap(
                        value -> Mono.fromCallable(() -> block(value))
                                .subscribeOn(scheduler),
                        4)
                .collect(Result::new, Result::add)
                .doOnSuccess(Result::stop);

    }

    @RequestMapping("/serial")
    public Mono<Result> serial() {
        Scheduler scheduler = Schedulers.parallel();
        log.info("Handling /serial");
        return Flux.range(1, 10)
                .log()
                .map(this::block)
                .collect(Result::new, Result::add)
                .doOnSuccess(Result::stop)
                .subscribeOn(scheduler);
    }

    @RequestMapping("/netty")
    public Mono<Result> netty() {
        log.info("Handling /netty");
        return Flux.range(1, 10)
                .log()
                .flatMap(this::fetch)
                .collect(Result::new, Result::add)
                .doOnSuccess(Result::stop);

    }

    private HttpStatus block(int value) {
        return this.restTemplate.getForEntity(url, String.class, value).getStatusCode();
    }

    private Mono<HttpStatus> fetch(int value) {
        return this.client.perform(ClientWebRequestBuilders.get(url)).extract(ResponseExtractors.response(String.class))
                .map(response -> response.getStatusCode());
    }

}
