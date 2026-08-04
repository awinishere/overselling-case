package com.example.demo.product;

import com.example.demo.domain.Product;
import com.example.demo.domain.percentage.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private final Long PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.save(new Product(PRODUCT_ID, "chair", 1));
    }

    @Test
    @DisplayName("Unsafe Method – Vulnerable to Race Conditions")
    void testRaceCondition() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    boolean success = productService.purchaseUnsafe(PRODUCT_ID, 1);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Product updatedProduct = productRepository.findById(PRODUCT_ID)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        assertNotEquals(1, successCount.get(), "An unsafe method should result in overselling");
    }

    @Test
    @DisplayName("Safe Method - Safe from Condition Pessimistic Locking")
    void testRaceCondition_Safe() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    boolean success = productService.purchaseSafe(PRODUCT_ID, 1);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Product updatedProduct = productRepository.findById(PRODUCT_ID).orElseThrow();

        assertEquals(1, successCount.get(), "Only one successful transaction is allowed!");
        assertEquals(9, failedCount.get(), "The remaining transactions must fail!");
        assertEquals(0, updatedProduct.getStock(), "The final stock in the database must be exactly zero!");
    }
}