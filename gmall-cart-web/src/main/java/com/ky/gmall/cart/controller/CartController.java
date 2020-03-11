package com.ky.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.ky.gmall.annotations.LoginRequired;
import com.ky.gmall.beans.OmsCartItem;
import com.ky.gmall.beans.PmsSkuInfo;
import com.ky.gmall.service.CartService;
import com.ky.gmall.service.SkuService;
import com.ky.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Controller
@CrossOrigin
public class CartController {

    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;


    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked,String skuId,HttpServletRequest request,ModelMap map){
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        //调用服务,修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        cartService.checkCart(omsCartItem);
        //将最新的数据从缓存中查出,渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        map.put("cartList",omsCartItems);
        //被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        map.put("totalAmount",totalAmount);
        return "cartListInner";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            if (omsCartItem.getIsChecked().equals("1")){
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap map){
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        if (StringUtils.isNotBlank(memberId)){
            //已经登录,查询db
            omsCartItems = cartService.cartList(memberId);
        }else {
            //没有登录,查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }

        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(BigDecimal.valueOf(omsCartItem.getQuantity())));
        }

        map.put("cartList",omsCartItems);
        //被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        map.put("totalAmount",totalAmount);
        return "cartList";
    }

    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        //调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);
        modelMap.put("skuInfo",skuInfo);

        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(quantity);

        //判断用户是否登录
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        if(StringUtils.isBlank(memberId)){
            //用户未登录
            //Cookie原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);

            if (StringUtils.isBlank(cartListCookie)){
                //cookie为空
                omsCartItems.add(omsCartItem);
            }else {
                //cookie不为空
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                //判断添加的购物车数据在Cookie中是否存在
                boolean exist = if_cart_exist(omsCartItems,omsCartItem);
                if (exist){
                    //之前添加过,更新购物车添加数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(omsCartItem.getQuantity() + cartItem.getQuantity());
                        }
                    }
                }else {
                    //之前没添加过,新增当前的购物车
                    omsCartItems.add(omsCartItem);
                }
            }
            //更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);
        }else{
            //用户已经登录
            //从db中查询购物车数据
            OmsCartItem omsCartItemFromDb = cartService.ifCartExistByUser(memberId,skuId);
            if (omsCartItemFromDb == null){
                //说明该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname(nickname);
                cartService.addCart(omsCartItem);
            }else{
                //该用户添加过当前商品
                omsCartItemFromDb.setQuantity(omsCartItem.getQuantity() + omsCartItemFromDb.getQuantity());
                cartService.updateCart(omsCartItemFromDb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);

        }

        return "redirect:http://cart.gmall.com:8084/success.html";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        for (OmsCartItem cartItem : omsCartItems) {
            String productSkuId = cartItem.getProductSkuId();
            //循环判断是否原来添加过该sku
            if (productSkuId.equals(omsCartItem.getProductSkuId())){
                return true;
            }
        }
        return false;
    }

}
