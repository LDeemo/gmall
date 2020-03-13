package com.ky.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.OmsOrder;
import com.ky.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Reference
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");

        //更新订单业务状态
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(out_trade_no);

        orderService.updateOrder(omsOrder);
    }

}
