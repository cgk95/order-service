package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.Objects;

@DataR2dbcTest
@Import(DataConfig.class)
@Testcontainers
public class OrderRepositoryR2dbcTests {
    @Container
    static PostgreSQLContainer<?> postgreSql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14:10"));

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource // 테스트 PostgreSQL 인스턴스에 연결하도록 R2DBC 와 플라이웨이 설정을 변경
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgreSql::getUsername);
        registry.add("spring.r2dbc.password", postgreSql::getPassword);
        registry.add("spring.flyway.url", postgreSql::getJdbcUrl);
    }

    /**
     * 테스트 PostgreSQL 인스턴스에 대한 R2DBC URL을 반환한다.(테스트 컨테이너가 R2DBC 에 대해서는 연결 문자열을 제공하지 않는다)
     *
     * @return R2DBC URL
     */
    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s",
                postgreSql.getHost(),
                postgreSql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgreSql.getDatabaseName());
    }

    @Test
    void createRejectedOrder() {
        Order order = OrderService.buildRejectedOrder("12345676798", 3);
        StepVerifier
                .create(orderRepository.save(order))
                .expectNextMatches(
                        o -> o.status().equals(OrderStatus.REJECTED)
                )
                .verifyComplete();
    }

    @Test
    void whenCreateOrderNotAuthenticatedThenNoAuditMetadata() {
        var rejectedOrder = OrderService.buildRejectedOrder( "1234567890", 3);
        StepVerifier.create(orderRepository.save(rejectedOrder))
                .expectNextMatches(order -> Objects.isNull(order.createdBy()) &&
                        Objects.isNull(order.lastModifiedBy()))
                .verifyComplete();
    }

    @Test
    @WithMockUser("marlena")
    void whenCreateOrderAuthenticatedThenAuditMetadata() {
        var rejectedOrder = OrderService.buildRejectedOrder( "1234567890", 3);
        StepVerifier.create(orderRepository.save(rejectedOrder))
                .expectNextMatches(order -> order.createdBy().equals("marlena") &&
                        order.lastModifiedBy().equals("marlena"))
                .verifyComplete();
    }
}
