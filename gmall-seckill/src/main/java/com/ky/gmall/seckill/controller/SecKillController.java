package com.ky.gmall.seckill.controller;

import com.ky.gmall.util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedissonClient redissonClient;


    /**
     * 用Redisson进行抢购
     * @return
     */
    @RequestMapping("secKill")
    @ResponseBody
    public String secKill(){
        //semaphore,信标(要去查询并修改的值),tryAcquire,尝试抢购,可能成功或许失败
        RSemaphore semaphore = redissonClient.getSemaphore("107");
        boolean b = semaphore.tryAcquire();
        if (b) {
            //抢购成功
            System.out.println("用户抢购成功===============");
            //用消息队列发出订单消息
        }else {
            System.out.println("用户抢购失败");
        }
        return "1";
    }


    /**
     * 使用redis的事务进行抢购
     * @return
     */
    @RequestMapping("kill")
    @ResponseBody
    public String kill() {
        String memberId = "1";
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            //开启商品监控
            jedis.watch("107");
            int stock = Integer.parseInt(jedis.get("107"));
            if (stock > 0) {
                Transaction multi = jedis.multi();
                multi.incrBy("107", -1);
                List<Object> exec = multi.exec();
                if (exec != null && exec.size() > 0) {
                    //抢购成功
                    System.out.println("当前库存剩余数量: " + stock + ",用户: " + memberId + "抢购成功,当前抢购人数: " + (1000 - stock));
                    //用消息队列发出订单消息
                }else {
                    System.out.println("当前库存剩余数量: " + stock + ",用户: " + memberId + "抢购失败");
                }
            }
        } finally {
            jedis.close();
        }

        return "1";
    }

}

