package com.ky.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.ky.gmall.beans.PaymentInfo;
import com.ky.gmall.mq.ActiveMQUtil;
import com.ky.gmall.payment.mapper.PaymentInfoMapper;
import com.ky.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        //幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParam);
        if (StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus())&&paymentInfoResult.getPaymentStatus().equals("已支付")){
            return;
        }

        Example e = new Example(PaymentInfo.class);
        e.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            //第一个值表示是否使用事务,true代表使用,第二个值用于选择
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        try {
            paymentInfoMapper.updateByExampleSelective(paymentInfo,e);
            //支付成功后,引起系统服务->订单系统的更新->库存服务->物流
            //调用mq发送支付成功的消息
            Queue payment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();//hash结构的message
            mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());

            producer.send(mapMessage);

            session.commit();

        }catch (Exception ex){
            //消息回滚
            try {
                session.rollback();
            } catch (JMSException exc) {
                exc.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }

    }

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo,int count) {
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            //第一个值表示是否使用事务,true代表使用,第二个值用于选择
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        try {

            Queue payment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();//hash结构的message
            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setInt("count",count);
            //加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*30);
            producer.send(mapMessage);

            session.commit();

        }catch (Exception ex){
            //消息回滚
            try {
                session.rollback();
            } catch (JMSException exc) {
                exc.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }

    }

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {
        Map<String,Object> resultMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no",out_trade_no);
        request.setBizContent(JSON.toJSONString(requestMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("交易已创建,调用成功");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            resultMap.put("call_back_content",response.getMsg());
        } else {
            System.out.println("有可能交易未创建,调用失败");
        }

        return resultMap;
    }
}
