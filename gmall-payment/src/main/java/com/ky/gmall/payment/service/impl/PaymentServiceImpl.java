package com.ky.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.ky.gmall.beans.PaymentInfo;
import com.ky.gmall.mq.ActiveMQUtil;
import com.ky.gmall.payment.mapper.PaymentInfoMapper;
import com.ky.gmall.service.PaymentService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
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
}
