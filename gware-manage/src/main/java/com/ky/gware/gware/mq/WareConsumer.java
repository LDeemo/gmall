package com.ky.gware.gware.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;


import com.ky.gware.gware.bean.OmsOrder;
import com.ky.gware.gware.bean.OmsOrderItem;
import com.ky.gware.gware.bean.WmsWareOrderTask;
import com.ky.gware.gware.bean.WmsWareOrderTaskDetail;
import com.ky.gware.gware.enums.TaskStatus;
import com.ky.gware.gware.mapper.WareOrderTaskDetailMapper;
import com.ky.gware.gware.mapper.WareOrderTaskMapper;
import com.ky.gware.gware.mapper.WareSkuMapper;
import com.ky.gware.gware.service.GwareService;
import com.ky.gware.gware.util.ActiveMQUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.*;

/**
 * @param
 * @return
 */
@Component
public class WareConsumer {
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    JmsTemplate jmsTemplate;

    @Reference
    GwareService gwareService;


    @JmsListener(destination = "ORDER_PAY_QUEUE", containerFactory = "jmsQueueListener")
    public void receiveOrder(TextMessage textMessage) throws JMSException {
        String orderTaskJson = textMessage.getText();

        /***
         * 转化并保存订单对象
         */
        OmsOrder orderInfo = JSON.parseObject(orderTaskJson, OmsOrder.class);

        // 将order订单对象转为订单任务对象
        WmsWareOrderTask wareOrderTask = new WmsWareOrderTask();
        wareOrderTask.setConsignee(orderInfo.getReceiverName());
        wareOrderTask.setConsigneeTel(orderInfo.getReceiverPhone());
        wareOrderTask.setCreateTime(new Date());
        wareOrderTask.setDeliveryAddress(orderInfo.getReceiverDetailAddress());
        wareOrderTask.setOrderId(orderInfo.getId());
        ArrayList<WmsWareOrderTaskDetail> wareOrderTaskDetails = new ArrayList<>();

        // 打开订单的商品集合
        List<OmsOrderItem> orderDetailList = orderInfo.getOmsOrderItems();
        for (OmsOrderItem orderDetail : orderDetailList) {
            WmsWareOrderTaskDetail wareOrderTaskDetail = new WmsWareOrderTaskDetail();

            wareOrderTaskDetail.setSkuId(orderDetail.getProductSkuId());
            wareOrderTaskDetail.setSkuName(orderDetail.getProductName());
            wareOrderTaskDetail.setSkuNum(orderDetail.getProductQuantity());
            wareOrderTaskDetails.add(wareOrderTaskDetail);

        }
        wareOrderTask.setDetails(wareOrderTaskDetails);
        wareOrderTask.setTaskStatus(TaskStatus.PAID);
        gwareService.saveWareOrderTask(wareOrderTask);

        textMessage.acknowledge();

        // 检查该交易的商品是否有拆单需求
        List<WmsWareOrderTask> wareSubOrderTaskList = gwareService.checkOrderSplit(wareOrderTask);// 检查拆单

        // 库存削减
        if (wareSubOrderTaskList != null && wareSubOrderTaskList.size() >= 2) {
            for (WmsWareOrderTask orderTask : wareSubOrderTaskList) {
                gwareService.lockStock(orderTask);
            }
        } else {
            gwareService.lockStock(wareOrderTask);
        }


    }

}
