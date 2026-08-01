package com.aics.order.controller;

import com.aics.order.dto.CartUpdateDTO;
import com.aics.order.service.CartService;
import com.aics.order.vo.CartVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 购物车控制器接口测试
 */
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @Test
    @DisplayName("GET /cart/list - 获取购物车列表")
    void getCartList_shouldReturn200() throws Exception {
        CartVO cartVO = new CartVO();
        cartVO.setItems(Collections.emptyList());
        cartVO.setTotalAmount(BigDecimal.ZERO);
        cartVO.setSelectedCount(0);

        when(cartService.getCartList(anyLong())).thenReturn(cartVO);

        mockMvc.perform(get("/cart/list")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("PUT /cart/quantity - 修改数量成功")
    void updateQuantity_shouldReturn200() throws Exception {
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setCartItemId(1L);
        dto.setQuantity(3);

        CartVO cartVO = new CartVO();
        cartVO.setItems(Collections.emptyList());
        cartVO.setTotalAmount(new BigDecimal("597.00"));
        cartVO.setSelectedCount(0);

        when(cartService.updateQuantity(anyLong(), eq(1L), eq(3))).thenReturn(cartVO);

        mockMvc.perform(put("/cart/quantity")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /cart/{id} - 删除购物车商品")
    void deleteCartItem_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/cart/1")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /cart/select - 切换选中状态")
    void selectCartItems_shouldReturn200() throws Exception {
        mockMvc.perform(put("/cart/select")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[1,2],\"selected\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
