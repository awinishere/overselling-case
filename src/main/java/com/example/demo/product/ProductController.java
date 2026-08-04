package com.example.demo.product;

import com.example.demo.product.dto.ProductRequest;
import com.example.demo.product.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping("/buy-unsafe")
    public ResponseEntity<ProductResponse> buyUnsafe(@Valid @RequestBody ProductRequest request){
        boolean success = productService.purchaseUnsafe(request.productId(), request.quantity());

        if (success){
            return ResponseEntity.ok(new ProductResponse(
                    true,
                    "Pembelian berhasil",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(new ProductResponse(
                    false,
                    "Out of stock",
                    null
            ));
        }
    }

    @PostMapping("/buy-safe")
    public ResponseEntity<ProductResponse> buySafe(@Valid @RequestBody ProductRequest request){
        boolean success = productService.purchaseSafe(request.productId(), request.quantity());

        if (success){
            return ResponseEntity.ok(new ProductResponse(
                    true,
                    "Purchase successful",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(new ProductResponse(
                    false,
                    "Out of Stock",
                    null
            ));
        }
    }
}