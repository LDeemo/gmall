package com.ky.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.ky.gmall.service.OrderService;
import com.ky.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

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
    public boolean checkPrice() {
        return false;
    }
}
