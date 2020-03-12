package com.ky.gmall.service;

import com.ky.gmall.beans.OmsCartItem;

import java.util.List;

public interface CartService {
    void flushCartCache(String memberId);

    void updateCart(OmsCartItem omsCartItemFromDb);

    void addCart(OmsCartItem omsCartItem);

    OmsCartItem ifCartExistByUser(String memberId, String skuId);

    List<OmsCartItem> cartList(String userId);

    void checkCart(OmsCartItem omsCartItem);

    void delCart(String productSkuId);
}
