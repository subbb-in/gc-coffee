package edu.example.dev_coffee2.service;

import edu.example.dev_coffee2.dto.OrderDTO;
import edu.example.dev_coffee2.dto.OrderItemDTO;
import edu.example.dev_coffee2.entity.Order;
import edu.example.dev_coffee2.entity.OrderItem;
import edu.example.dev_coffee2.entity.Product;
import edu.example.dev_coffee2.enums.OrderStatus;
import edu.example.dev_coffee2.exception.OrderException;
import edu.example.dev_coffee2.repository.OrderItemRepository;
import edu.example.dev_coffee2.repository.OrderRepository;
import edu.example.dev_coffee2.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public void add(OrderItemDTO orderItemDTO){   //등록
        Optional<Order> foundOrder = orderRepository.findByEmail(orderItemDTO.getEmail());

        Order savedOrder = foundOrder.orElseGet(() -> {
            Order cart = Order.builder()
                    .email(orderItemDTO.getEmail())
                    .address(orderItemDTO.getAddress())
                    .postcode(orderItemDTO.getPostcode())
                    .build();
            return orderRepository.save(cart);
        });

        Product foundProduct = productRepository.findById(orderItemDTO.getProductId())
                .orElseThrow(
                        OrderException.PRODUCT_NOT_FOUND::get);

        Optional<List<OrderItem>> foundOrderItems = orderItemRepository.getOrderItems(orderItemDTO.getEmail());
        List<OrderItem> orderItems = foundOrderItems.orElse(null);

        boolean found = false;

        for (OrderItem orderItem : orderItems) {
            if(orderItem.getProduct() == foundProduct){
                int newQuantity = orderItem.getQuantity() + orderItemDTO.getQuantity();
                int newPrice = orderItem.getPrice() + orderItemDTO.getPrice();

                orderItem.changeQuantity(newQuantity);
                orderItem.changePrice(newPrice);

                try {
                    orderItemRepository.save(orderItem);
                } catch(Exception e) {
                    log.error("--- " + e.getMessage());
                    throw OrderException.FAIL_ADD.get();
                }
                found = true;
                break;
            }
        }

        if (!found) {
            OrderItem orderItem = OrderItem.builder()
                    .quantity(orderItemDTO.getQuantity())
                    .price(foundProduct.getPrice() * orderItemDTO.getQuantity())
                    .product(foundProduct)
                    .order(savedOrder)
                    .build();

            try {
                orderItemRepository.save(orderItem);
            } catch(Exception e) {
                log.error("--- " + e.getMessage());
                throw OrderException.FAIL_ADD.get();
            }
        }

    }

    public OrderDTO getOrderNItems(String email){
        Optional<Order> foundOrder = orderRepository.findByEmail(email);
        if (!foundOrder.isPresent()) {
            throw OrderException.NOT_FOUND_ORDER.get();
        }
        Order order = foundOrder.get();

        Optional<List<OrderItem>> foundOrderItems = orderItemRepository.getOrderItems(email);
        if (!foundOrderItems.isPresent()) {
            throw OrderException.NOT_FOUND_ORDERITEM.get();
        }

        List<OrderItem> orderItemList = foundOrderItems.get();

        return new OrderDTO(order, orderItemList);

    }

    public void modify(OrderItemDTO orderItemDTO){    //수정
        OrderItem orderItem = orderItemRepository.findById(orderItemDTO.getOrderItemId()).orElseThrow(
                OrderException.NOT_FOUND_ORDERITEM::get
        );

        if(orderItemDTO.getQuantity() <= 0){
            try{
                orderItemRepository.deleteById(orderItemDTO.getOrderItemId());
                return;
            }catch (Exception e){
                log.error("--- " + e.getMessage());
                throw OrderException.FAIL_REMOVE.get();
            }
        }

        try {
            orderItem.changeQuantity(orderItemDTO.getQuantity());
            orderItem.changePrice(orderItemDTO.getQuantity() * orderItem.getProduct().getPrice());
        } catch(Exception e) {
            log.error("--- " + e.getMessage());
            throw OrderException.FAIL_MODIFY.get();
        }

        orderItemRepository.save(orderItem);
    }

    //삭제
    public void remove(Long orderItemId){
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElseThrow(
                                        OrderException.NOT_FOUND_ORDERITEM::get);

        try {
            orderItemRepository.deleteById(orderItemId);

            Optional<List<OrderItem>> foundOrderItems = orderItemRepository.getOrderItems(orderItem.getOrder().getOrderId());
            if (foundOrderItems.isEmpty() || foundOrderItems.get().size() == 0) {
                orderRepository.deleteById(orderItem.getOrder().getOrderId());
            }

        } catch(Exception e) {
            log.error("--- " + e.getMessage());
            throw OrderException.FAIL_REMOVE.get();
        }

    }

    // 시간에 따른 OrderStatus 자동 변환 메서드
    @Scheduled(cron = "0 0 2 * * ?") // 매일 02시에 실행
    public void updateOrderStatusesReady() {
        // 어제 14시
        LocalDateTime start = LocalDateTime.now().minusDays(1).withHour(14).withMinute(0).withSecond(0);

        // 오늘 02시
        LocalDateTime end = LocalDateTime.now().withHour(2).withMinute(0).withSecond(0);

        // 조건에 맞는 주문 조회
        List<Order> orders = orderRepository.findByRegDateBetweenAndOrderStatusNot(start, end, OrderStatus.READY_FOR_DELIVERY);

        for (Order order : orders) {
            order.changeOrderStatus(OrderStatus.READY_FOR_DELIVERY);
            orderRepository.save(order);
        }
    }

}
