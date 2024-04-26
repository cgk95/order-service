package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.event.OrderDispatchedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    private final BookClient bookClient;

    private final StreamBridge streamBridge;

    @Autowired
    public OrderService(OrderRepository orderRepository, BookClient bookClient, StreamBridge streamBridge) {
        this.orderRepository = orderRepository;
        this.bookClient = bookClient;
        this.streamBridge = streamBridge;
    }

    public Flux<Order> getAllOrders(String userId) {
        return orderRepository.findALlByCreatedBy(userId);
    }


    public Mono<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id);
    }

    @Transactional // 메서드를 로컬 트랜잭션으로 실행한다
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty( // 책이 존재하지 않으면 주문을 거부한다
                        buildRejectedOrder(isbn, quantity)
                )
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent); // 주문이 수락되면 이벤트를 발행한다
    }

    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux.flatMap(message ->
                        orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }

    public Order buildDispatchedOrder(Order order) {
        return new Order(
                order.id(),
                order.bookIsbn(),
                order.bookName(),
                order.bookPrice(),
                order.quantity(),
                OrderStatus.DISPATCHED,
                order.createdDate(),
                order.lastModifiedDate(),
                order.createdBy(),
                order.lastModifiedBy(),
                order.version()
        );
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) {
            return;
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted message for order with id {}", order.id());
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Result of sending order accepted message: {}", result);
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(book.isbn(), book.title() + "-" + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
    }

    public static Order buildRejectedOrder(String isbn, int quantity) {
        return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
    }


}
