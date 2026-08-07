package com.aics.order.controller;

import com.aics.common.result.Result;
import com.aics.order.bo.PriceCalcBO;
import com.aics.order.dto.CartUpdateDTO;
import com.aics.order.dto.CheckoutDTO;
import com.aics.order.entity.CartItem;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.service.CartService;
import com.aics.order.service.PromotionService;
import com.aics.order.vo.CartVO;
import com.aics.order.vo.CheckoutConfirmVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车控制器
 */
@Tag(name = "购物车管理", description = "购物车商品增删改查")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final PromotionService promotionService;
    private final CartItemMapper cartItemMapper;

    @Operation(summary = "获取购物车列表")
    @GetMapping("/list")
    public Result<CartVO> getCartList(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.getCartList(userId));
    }

    @Operation(summary = "修改商品数量")
    @PutMapping("/quantity")
    public Result<CartVO> updateQuantity(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody CartUpdateDTO dto) {
        return Result.success(cartService.updateQuantity(userId, dto.getCartItemId(), dto.getQuantity()));
    }

    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{cartItemId}")
    public Result<Void> deleteCartItem(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable("cartItemId") Long cartItemId) {
        cartService.deleteCartItem(userId, cartItemId);
        return Result.success();
    }

    @Operation(summary = "切换商品选中状态")
    @PutMapping("/select")
    public Result<Void> selectCartItems(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> cartItemIds = ((List<Integer>) body.get("cartItemIds"))
                .stream().map(Long::valueOf).toList();
        Boolean selected = (Boolean) body.get("selected");
        cartService.selectCartItems(userId, cartItemIds, selected);
        return Result.success();
    }

    @Operation(summary = "获取结算确认页信息")
    @PostMapping("/checkout/confirm")
    public Result<CheckoutConfirmVO> checkoutConfirm(@RequestHeader("X-User-Id") Long userId,
                                                     @Valid @RequestBody CheckoutDTO dto) {
        List<CartItem> items = cartItemMapper.selectBatchIds(dto.getCartItemIds());

        // 计算商品总额
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算优惠
        PriceCalcBO priceCalc = promotionService.calculatePrice(totalAmount, userId, null);

        // 获取可用优惠券
        List<PromotionService.CouponAvailability> coupons =
                promotionService.getAvailableCoupons(userId, totalAmount);

        // 组装 VO
        CheckoutConfirmVO vo = new CheckoutConfirmVO();
        vo.setItems(items.stream().map(item -> {
            CartVO.CartItemVO itemVO = new CartVO.CartItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSelected(item.getSelected());
            itemVO.setSubtotal(item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemVO;
        }).collect(Collectors.toList()));
        vo.setTotalAmount(totalAmount);

        CheckoutConfirmVO.FullReductionVO frVO = new CheckoutConfirmVO.FullReductionVO();
        frVO.setApplied(priceCalc.getFullReductionAmount().compareTo(BigDecimal.ZERO) > 0);
        frVO.setRuleName(priceCalc.getFullReductionRuleName());
        frVO.setAmount(priceCalc.getFullReductionAmount());
        vo.setFullReduction(frVO);

        vo.setAvailableCoupons(coupons.stream().map(c -> {
            CheckoutConfirmVO.CouponVO cVO = new CheckoutConfirmVO.CouponVO();
            cVO.setId(c.getId());
            cVO.setCouponName(c.getCouponName());
            cVO.setAmount(c.getAmount());
            cVO.setMinOrderAmount(c.getMinOrderAmount());
            cVO.setUsable(c.isUsable());
            cVO.setReason(c.getReason());
            return cVO;
        }).collect(Collectors.toList()));

        vo.setPayAmount(priceCalc.getPayAmount());

        return Result.success(vo);
    }
}
