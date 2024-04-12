package com.polarbookshop.orderservice.book;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@TestMethodOrder(MethodOrderer.Random.class)
public class BookClientTests {
    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        var webClient = WebClient.builder()
                                 .baseUrl(mockWebServer.url("/").uri().toString())
                                 .build();
        this.bookClient = new BookClient(webClient);
    }

    @AfterEach
    void clean() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Test
    void whenBookExistsThenReturnBook() {
        var bookIsbn = "1234567890";

        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                                {
                                "isbn": %s,
                                "title": "test Title",
                                "author": "Author",
                                "price": 9.99,
                                "publisher":"Polar sophia"
                                }
                        """.formatted(bookIsbn)
                );
        mockWebServer.enqueue(mockResponse);

        Mono<Book> book = bookClient.getBookByIsbn(bookIsbn);

        StepVerifier.create(book) // BookClient 가 반환하는 객체로 StepVerifier를 초기화 한다.
                    .expectNextMatches(
                            b -> b.isbn().equals(bookIsbn) // 반환된 책의 isbn을 검증한다
                    )
                    .verifyComplete(); // 리액티브 스트림이 성공적으로 완료되었는지 확인
    }
}
