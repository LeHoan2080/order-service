package com.springmicroservice.orderservice.service;

import com.springmicroservice.orderservice.dto.InventoryRespone;
import com.springmicroservice.orderservice.dto.OrderLineItemsDto;
import com.springmicroservice.orderservice.dto.OrderRequest;
import com.springmicroservice.orderservice.model.Order;
import com.springmicroservice.orderservice.model.OrderLineItems;
import com.springmicroservice.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::getOrderLineItems)
                .toList();
        order.setOrderLineItems(orderLineItems);

        List<String> skuCodes = order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        //Gọi tới kho v kiểm tra nếu có trong kho thì gọi ra
        InventoryRespone[] inventoryRespones = webClient.get()
                .uri("http://localhost:8030/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCodes", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryRespone[].class)
                .block();

        boolean allProductsInStock;
        if (inventoryRespones.length == 0) {
            allProductsInStock = false;
        }else{
            allProductsInStock = Arrays.stream(inventoryRespones)
                    .allMatch(InventoryRespone::isInStock);
        }

        if (allProductsInStock) {
            orderRepository.save(order);
        }else {
            throw new IllegalArgumentException("Order could not be placed, please try again");
        }
    }

    private OrderLineItems getOrderLineItems(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
