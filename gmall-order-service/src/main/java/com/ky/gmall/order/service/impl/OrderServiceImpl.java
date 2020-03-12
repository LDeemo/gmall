package com.ky.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.ky.gmall.beans.OmsOrder;
import com.ky.gmall.beans.OmsOrderItem;
import com.ky.gmall.order.mapper.OmsOrderItemMapper;
import com.ky.gmall.order.mapper.OmsOrderMapper;
import com.ky.gmall.service.CartService;
import com.ky.gmall.service.OrderService;
import com.ky.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Reference
    CartService cartService;

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";

            //使用lua脚本在发现key的同时将key删除,防止并发订单攻击
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            if (eval != null && eval != 0) {
                //jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }

        } finally {
            jedis.close();
        }
    }

    @Override
    public String genTradeCode(String memberId) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();

            String tradeKey = "user:" + memberId + ":tradeCode";

            String tradeCode = UUID.randomUUID().toString().replaceAll("-", "");

            jedis.setex(tradeKey, 60 * 15, tradeCode);

            return tradeCode;

        } finally {
            jedis.close();
        }
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        //保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        //保存订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            //删除购物车
            //cartService.delCart(omsOrderItem.getProductSkuId());
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);
        return omsOrder1;
    }
}
