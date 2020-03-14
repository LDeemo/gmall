package com.ky.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.PaymentInfo;
import com.ky.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Reference
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");
        int count = mapMessage.getInt("count");

        //调用paymentService的支付宝检查接口
        Map<String, Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);

        if (resultMap != null && !resultMap.isEmpty()) {
            String trade_status = (String) resultMap.get("trade_status");
            //根据查询的支付状态结果,判断是否进行下一次延迟任务还是支付成功更新数据和后续任务
            if (StringUtils.isNotBlank(trade_status) && trade_status.equals("TRADE_SUCCESS")) {
                //支付成功,更新支付发送支付队列
                System.out.println("支付成功,调用支付服务,修改支付信息,发送支付成功队列");
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn((String) resultMap.get("out_trade_no"));
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no")); //支付宝的交易凭证号
                paymentInfo.setCallbackContent((String)resultMap.get("call_back_content")); //回调请求字符串
                paymentInfo.setCallbackTime(new Date());
                paymentService.updatePayment(paymentInfo);
                return;
            }
        }

        if (count > 0) {
            //继续发送延迟检查任务,计算延迟时间等
            count--;
            System.out.println("没有支付成功,检查次数剩余" + count + "次,继续发送延迟检查任务");
            paymentService.sendDelayPaymentResultCheckQueue(out_trade_no, count);
        } else {
            System.out.println("检查次数用尽,停止检查");
        }
    }


}

