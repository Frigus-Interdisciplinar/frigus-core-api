package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.product.ProductCreateRequestDto;
import com.frigus.coreapi.dto.product.ProductResponseDto;
import com.frigus.coreapi.dto.product.ProductUpdateRequestDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;
import com.frigus.coreapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock private ProductService productService;
    @InjectMocks private ProductController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void delegatesAllProductEndpoints() {
        PageRequest page = PageRequest.of(0, 10);
        ProductResponseDto product = ProductResponseDto.builder().id(5).name("Leite").build();
        ProductCreateRequestDto create = createRequest();
        ProductUpdateRequestDto update = updateRequest();
        when(productService.findAll(page)).thenReturn(new PageImpl<>(List.of(product)));
        when(productService.findById(5)).thenReturn(product);
        when(productService.create(create)).thenReturn(product);
        when(productService.update(5, update)).thenReturn(product);

        assertThat(controller.findAll(page).getContent()).containsExactly(product);
        assertThat(controller.findById(5)).isSameAs(product);
        assertThat(controller.create(create)).isSameAs(product);
        assertThat(controller.update(5, update)).isSameAs(product);
        controller.delete(5);

        verify(productService).delete(5);
    }

    @Test
    void rejectsInvalidCreatePayloadBeforeCallingService() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(productService, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void exposesPublicReadRoutes() throws NoSuchMethodException {
        RequestMapping controllerMapping = ProductController.class.getAnnotation(RequestMapping.class);
        GetMapping listMapping = ProductController.class.getMethod("findAll", Pageable.class)
                .getAnnotation(GetMapping.class);
        GetMapping itemMapping = BaseController.class.getMethod("findById", Object.class)
                .getAnnotation(GetMapping.class);

        assertThat(controllerMapping.value()).containsExactly("/products");
        assertThat(listMapping).isNotNull();
        assertThat(itemMapping.value()).containsExactly("/{id}");
    }

    @Test
    void restrictsCatalogMutationsToAdmins() throws NoSuchMethodException {
        assertAdminOnly("create", ProductCreateRequestDto.class);
        assertAdminOnly("update", Integer.class, ProductUpdateRequestDto.class);
        assertAdminOnly("delete", Integer.class);
    }

    private void assertAdminOnly(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        PreAuthorize authorization = ProductController.class.getMethod(name, parameterTypes)
                .getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");
    }

    private ProductCreateRequestDto createRequest() {
        return ProductCreateRequestDto.builder()
                .name("Leite")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .build();
    }

    private ProductUpdateRequestDto updateRequest() {
        return ProductUpdateRequestDto.builder()
                .name("Leite")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .build();
    }
}
