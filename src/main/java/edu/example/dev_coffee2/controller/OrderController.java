package edu.example.dev_coffee2.controller;

import edu.example.dev_coffee2.dto.OrderDTO;
import edu.example.dev_coffee2.dto.OrderItemDTO;
import edu.example.dev_coffee2.entity.Product;
import edu.example.dev_coffee2.exception.OrderException;
import edu.example.dev_coffee2.exception.ProductException;
import edu.example.dev_coffee2.service.OrderService;
import edu.example.dev_coffee2.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Log4j2
@Tag(name = "Order Controller", description = "주문과 주문 목록 컨트롤러")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;

    @Operation(summary = "주문 추가", description = "주문 추가를 진행합니다.")
    @PostMapping
    public ResponseEntity<List<OrderItemDTO>> add(@RequestBody OrderItemDTO orderItemDTO) {

        Product product = productService.read(orderItemDTO.getProductId()).toEntity();
        if(product == null || !product.getProductName().equals(orderItemDTO.getProductName())) {
            throw ProductException.NOT_FOUND.get();
        }

        if(orderItemDTO.getPrice() != product.getPrice() * orderItemDTO.getQuantity() ){
            throw OrderException.NOT_MATCHED_PRICE.get();
        }

        orderService.add(orderItemDTO);
        return null;
    }

    @Operation(summary = "주문 조회", description = "주문 조회 진행합니다.")
    @GetMapping("/{email}")
    public ResponseEntity<OrderDTO> get(@PathVariable String email) {
        return ResponseEntity.ok(orderService.getOrderNItems(email));
    }

    @Operation(summary = "주문 수정", description = "주문 수정을 진행합니다.")
    @PutMapping("/{orderItemId}")
    public ResponseEntity<?> modify(@RequestBody OrderItemDTO orderItemDTO,
                                    @PathVariable("orderItemId") Long orderItemId) {
        try{
            orderService.modify(orderItemDTO);
            return ResponseEntity.ok().build();
        }catch (Exception e){
            throw OrderException.FAIL_MODIFY.get();
        }
    }

    @Operation(summary = "주문 삭제", description = "주문 삭제를 진행합니다.")
    @DeleteMapping("/{orderItemId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("orderItemId") Long orderItemId) {
        orderService.remove(orderItemId);
        return ResponseEntity.ok(Map.of("message", "Order deleted"));
    }

}

