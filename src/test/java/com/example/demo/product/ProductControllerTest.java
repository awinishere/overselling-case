package com.example.demo.product;

import com.example.demo.product.dto.ProductRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ProductController productController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Must return a 400 Bad Request if the request input is invalid.")
    void whenInputIsInvalid_thenReturns400() throws Exception {
        ProductRequest invalidRequest = new ProductRequest(null, -1);

        mockMvc.perform(post("/api/v1/products/buy-unsafe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Must return 200 OK if the input is valid and the transaction is successful.")
    void whenInputIsValid_thenReturns200() throws Exception {
        ProductRequest validRequest = new ProductRequest(1L, 5);

        when(productService.purchaseUnsafe(eq(1L), eq(5))).thenReturn(true);

        mockMvc.perform(post("/api/v1/products/buy-unsafe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }
}