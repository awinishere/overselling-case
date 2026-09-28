package com.example.demo.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public boolean purchaseUnsafe(Long productId, Integer quantity){
        log.info("Processing unsafe purchases for ID products: {}, amount: {}", productId, quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Produk ID {} not found", productId);
                    return new RuntimeException("Product not found");
                });

        if (product.getStock() >= quantity){
            try{
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            log.info("Unsafe purchase successful. Remaining stock: {}", product.getStock());
            return true;
        }
        log.warn("Insufficient stock of product {}. Stock: {}, Requested: {}", productId, product.getStock(), quantity);
        return false;
    }

    @Transactional
    public boolean purchaseSafe(Long productId, Integer quantity){
        log.info("Processing safe purchase for product ID: {}, quantity: {}", productId, quantity);
        Product product = productRepository.findByIdWithPessimisticLock(productId)
                .orElseThrow(() -> {
                    log.error("Produk ID {} not found", productId);
                    return new RuntimeException("Product not found");
                });

        if(product.getStock() >= quantity){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            log.info("Safe purchase successful. Remaining stock: {}", product.getStock());
            return true;
        }
        log.warn("Insufficient stock of product {} Stock: {}, Requested: {}", productId, product.getStock(), quantity);
        return false;
    }
}
