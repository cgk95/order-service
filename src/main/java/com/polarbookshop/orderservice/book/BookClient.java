package com.polarbookshop.orderservice.book;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class BookClient {
    private static final String BOOKS_ROOT_API = "/books";
    private final WebClient webClient;

    @Autowired
    public BookClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient.get()
                        .uri(BOOKS_ROOT_API + "/{isbn}", isbn)
                        .retrieve() // 요청을 보내고 응답을 받는다
                        .bodyToMono(Book.class)
                        .timeout(Duration.ofSeconds(3), Mono.empty()) // 3초 이내에 응답이 없으면 폴백으로 Mono.empty()를 반환한다
                        .onErrorResume(WebClientResponseException.NotFound.class, exception -> Mono.empty())
                        .retryWhen( // 지터 팩터
                                Retry.backoff(3, Duration.ofMillis(100))
                        )
                        .onErrorResume(Exception.class, exception -> Mono.empty());
    }
}
