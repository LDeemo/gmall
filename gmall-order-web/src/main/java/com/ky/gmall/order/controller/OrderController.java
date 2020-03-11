package com.ky.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.annotations.LoginRequired;
import com.ky.gmall.beans.OmsCartItem;
import com.ky.gmall.beans.OmsOrder;
import com.ky.gmall.beans.OmsOrderItem;
import com.ky.gmall.beans.UmsMemberReceiveAddress;
import com.ky.gmall.service.CartService;
import com.ky.gmall.service.OrderService;
import com.ky.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;
    @Reference
    UserService userService;
    @Reference
    OrderService orderService;



    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public String submitOrder(String receiveAddressId, String tradeCode,BigDecimal totalAmount ,HttpServletRequest request, ModelMap map){
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");


        //检查交易码
        String success = orderService.checkTradeCode(memberId,tradeCode);
        if (success.equals("success")){
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            OmsOrder omsOrder = new OmsOrder();
            //根据用户id获得要购买的商品列表(购物车),和总价格
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

            for (OmsCartItem omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1")){
                    //获得订单列表
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    //验价格
                    boolean b = orderService.checkPrice();
                    if (b == false){
                        return "tradeFail";
                    }
                    //验库存,远程调用库存系统
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setOrderSn("");//订单号
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setRealAmount(null);
                    omsOrderItems.add(omsOrderItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);




            //将订单和订单详情写入数据库
            //删除购物车的对应商品

        }else {
            return "tradeFail";
        }




        //重定向到支付系统
        return null;
    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap map){
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //收件人地址列表
        List<UmsMemberReceiveAddress> memberReceiveAddresses = userService.getReceiveAddressByMemberId(memberId);

        //集合转化为页面计算清单集合
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        for (OmsCartItem omsCartItem : omsCartItems) {
            //每循环一个购物车对象就封装一个商品详情到OmsOrderItem
            if (omsCartItem.getIsChecked().equals("1")){
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItems.add(omsOrderItem);
            }
        }
        BigDecimal totalAmount = getTotalAmount(omsOrderItems);
        map.put("memberReceiveAddresses",memberReceiveAddresses);
        map.put("totalAmount",totalAmount);
        map.put("nickname",nickname);
        map.put("omsOrderItems",omsOrderItems);

        //生成交易码,为了在提交订单时做校验
        String tradeCode = orderService.genTradeCode(memberId);
        map.put("tradeCode",tradeCode);
        return "trade";
    }

    private BigDecimal getTotalAmount(List<OmsOrderItem> omsOrderItems) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            BigDecimal omsOrderItemProductPricePrice = omsOrderItem.getProductPrice().multiply(BigDecimal.valueOf(omsOrderItem.getProductQuantity()));
            totalAmount = totalAmount.add(omsOrderItemProductPricePrice);
        }
        return totalAmount;
    }
}
