package com.ky.gware.gware.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;


import com.ky.gware.gware.bean.WmsWareInfo;
import com.ky.gware.gware.bean.WmsWareOrderTask;
import com.ky.gware.gware.bean.WmsWareOrderTaskDetail;
import com.ky.gware.gware.bean.WmsWareSku;
import com.ky.gware.gware.enums.TaskStatus;
import com.ky.gware.gware.mapper.WareInfoMapper;
import com.ky.gware.gware.mapper.WareOrderTaskDetailMapper;
import com.ky.gware.gware.mapper.WareOrderTaskMapper;
import com.ky.gware.gware.mapper.WareSkuMapper;
import com.ky.gware.gware.service.GwareService;
import com.ky.gware.gware.util.ActiveMQUtil;
import com.ky.gware.gware.util.HttpclientUtil;
import org.apache.activemq.command.ActiveMQMapMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;


import javax.jms.*;
import java.util.*;

@Service
public class GwareServiceImpl implements GwareService {

     @Autowired
     WareSkuMapper wareSkuMapper;

    @Autowired
    WareInfoMapper wareInfoMapper;

    @Autowired
    WareOrderTaskMapper wareOrderTaskMapper;

    @Autowired
    WareOrderTaskDetailMapper wareOrderTaskDetailMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Value("${order.split.url}")
    private String ORDER_URL;

     public Integer  getStockBySkuId(String skuid){
         Integer stock = wareSkuMapper.selectStockBySkuid(skuid);

         return stock;
     }


    public boolean  hasStockBySkuId(String skuid,Integer num){
        Integer stock = getStockBySkuId(  skuid);

        if(stock==null||stock<num){
            return false;
        }
        return true;
    }


    public List<WmsWareInfo> getWareInfoBySkuid(String skuid){
        List<WmsWareInfo> wareInfos = wareInfoMapper.selectWareInfoBySkuid(skuid);
        return wareInfos;
    }

    public List<WmsWareInfo> getWareInfoList(){
        List<WmsWareInfo> wareInfos = wareInfoMapper.selectAll();
        return wareInfos;
    }


    public void addWareInfo(){
        WmsWareInfo wareInfo =new WmsWareInfo();
        wareInfo.setAddress("1123");
        wareInfo.setAreacode("123123");
        wareInfo.setName("123123");
        wareInfoMapper.insertSelective(wareInfo);


        WmsWareSku wareSku =new WmsWareSku();
        wareSku.setId(wareInfo.getId());
        wareSku.setWarehouseId("991");
        wareSkuMapper.insertSelective(wareSku);
    }


    public   Map<String,List<String>>  getWareSkuMap(List<String> skuIdlist){
        Example example=new Example(WmsWareSku.class);
        example.createCriteria().andIn("skuId",skuIdlist);
        List<WmsWareSku> wareSkuList = wareSkuMapper.selectByExample(example);

        Map<String,List<String>> wareSkuMap=new HashMap<>();

        for (WmsWareSku wareSku : wareSkuList) {
            List<String>  skulistOfWare = wareSkuMap.get(wareSku.getWarehouseId());
            if (skulistOfWare==null){
                skulistOfWare=new ArrayList<>();
            }
            skulistOfWare.add(wareSku.getSkuId());
            wareSkuMap.put(wareSku.getWarehouseId(),skulistOfWare);
        }

        return  wareSkuMap;

    }


    public  List<Map<String,Object>>  convertWareSkuMapList( Map<String,List<String>>  wareSkuMap){

        List<Map<String,Object>> wareSkuMapList=new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : wareSkuMap.entrySet()) {
            Map<String,Object> skuWareMap=new HashMap<>();
            String wareid= entry.getKey();
            skuWareMap.put("wareId",wareid);
            List<String> skuids = entry.getValue();
            skuWareMap.put("skuIds",skuids);
            wareSkuMapList.add(skuWareMap);
        }
        return wareSkuMapList;

    }


    public void addWareSku(WmsWareSku wareSku){
        wareSkuMapper.insertSelective(wareSku);
    }

    public List<WmsWareSku> getWareSkuList(){
        List<WmsWareSku> wareSkuList = wareSkuMapper.selectWareSkuAll();
        return wareSkuList;
    }

    public WmsWareOrderTask getWareOrderTask(String taskId){

        WmsWareOrderTask wareOrderTask  = wareOrderTaskMapper.selectByPrimaryKey(taskId);

        WmsWareOrderTaskDetail wareOrderTaskDetail=new WmsWareOrderTaskDetail();
        wareOrderTaskDetail.setTaskId(taskId);
        List<WmsWareOrderTaskDetail> details = wareOrderTaskDetailMapper.select(wareOrderTaskDetail);
        wareOrderTask.setDetails(details);
        return wareOrderTask;
    }




    /***
     * 出库操作  减库存和锁定库存，
     * @param taskExample
     */
    @Transactional
    public void deliveryStock(WmsWareOrderTask  taskExample)  {
        String trackingNo = taskExample.getTrackingNo();
        WmsWareOrderTask wareOrderTask=getWareOrderTask(  taskExample.getId());
        wareOrderTask.setTaskStatus(TaskStatus.DELEVERED);
        List<WmsWareOrderTaskDetail> details = wareOrderTask.getDetails();
        for (WmsWareOrderTaskDetail detail : details) {
                WmsWareSku wareSku=new WmsWareSku();
                wareSku.setWarehouseId(wareOrderTask.getWareId());
                wareSku.setSkuId(detail.getSkuId());
                wareSku.setStock(detail.getSkuNum());
                wareSkuMapper.deliveryStock(wareSku);
        }

        wareOrderTask.setTaskStatus(TaskStatus.DELEVERED);
        wareOrderTask.setTrackingNo(trackingNo);
        wareOrderTaskMapper.updateByPrimaryKeySelective(wareOrderTask);
        try {
            sendToOrder(wareOrderTask);
        } catch(JMSException e){
            e.printStackTrace();
        }
    }


    public void sendToOrder(WmsWareOrderTask wareOrderTask) throws JMSException{
            Connection conn = activeMQUtil.getConnection();

            Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
            Destination destination = session.createQueue("SKU_DELIVER_QUEUE");
            MessageProducer producer = session.createProducer(destination);
            MapMessage mapMessage=new ActiveMQMapMessage();
            mapMessage.setString("orderId",wareOrderTask.getOrderId());
            mapMessage.setString("status",wareOrderTask.getTaskStatus().toString()); //小细节 枚举
            mapMessage.setString("trackingNo",wareOrderTask.getTrackingNo());

            producer.send(mapMessage);
            session.commit();

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public  List<WmsWareOrderTask>   checkOrderSplit(WmsWareOrderTask wareOrderTask) {
        List<WmsWareOrderTaskDetail> details = wareOrderTask.getDetails();
        List<String> skulist = new ArrayList<>();
        for (WmsWareOrderTaskDetail detail : details) {
            skulist.add(detail.getSkuId());
        }
        Map<String, List<String>> wareSkuMap = getWareSkuMap(skulist);
        // 一次物流运输无法完成订单
        if (wareSkuMap.entrySet().size()<2) {
            Map.Entry<String, List<String>> entry = wareSkuMap.entrySet().iterator().next();
            String wareid = entry.getKey();
            wareOrderTask.setWareId(wareid);
        } else {
            //需要拆单
            List<Map<String, Object>> wareSkuMapList = convertWareSkuMapList(wareSkuMap);
            String jsonString = JSON.toJSONString(wareSkuMapList);
            Map<String, String> map = new HashMap<>();
            map.put("orderId", wareOrderTask.getOrderId());
            map.put("wareSkuMap", jsonString);
            
            // 调用订单系统拆单接口
            String resultJson = HttpclientUtil.doPost(  ORDER_URL, map);

            List<WmsWareOrderTask> wareOrderTaskList = JSON.parseArray(resultJson, WmsWareOrderTask.class);
            
            if(wareOrderTaskList==null){
                wareOrderTaskList = new ArrayList<>();
                for (WmsWareOrderTaskDetail detail : details) {
                    WmsWareOrderTask wareOrderTask1 = new WmsWareOrderTask();

                    List<WmsWareOrderTaskDetail> wareOrderTaskDetails = new ArrayList<>();
                    wareOrderTaskDetails.add(detail);
                    wareOrderTask1.setDetails(wareOrderTaskDetails);
                    String skuId = detail.getSkuId();
                    WmsWareSku wareSku = new WmsWareSku();
                    wareSku.setSkuId(skuId);
                    wareSku.setStock(null);
                    List<WmsWareSku> select = wareSkuMapper.select(wareSku);
                    wareOrderTask1.setWareId(select.get(0).getWarehouseId());

                    wareOrderTaskList.add(wareOrderTask1);
                }
            }
            
            
            if (wareOrderTaskList.size()>=2){
//                for (WareOrderTask subOrderTask : wareOrderTaskList) {
//                    subOrderTask.setTaskStatus(TaskStatus.DEDUCTED);
//                    saveWareOrderTask(subOrderTask);
//                }
//                updateStatusWareOrderTaskByOrderId(wareOrderTask.getOrderId(),TaskStatus.SPLIT);
                return wareOrderTaskList;
            }else{
                throw new  RuntimeException("拆单异常!!");
            }

        }

        return  null;
    }


        public WmsWareOrderTask saveWareOrderTask(WmsWareOrderTask wareOrderTask ){
            wareOrderTask.setCreateTime(new Date());
            WmsWareOrderTask wareOrderTaskQuery=new WmsWareOrderTask();
            wareOrderTaskQuery.setOrderId(wareOrderTask.getOrderId());

            WmsWareOrderTask wareOrderTaskOrigin = wareOrderTaskMapper.selectOne(wareOrderTaskQuery);
            if(wareOrderTaskOrigin!=null){
                return  wareOrderTaskOrigin;
            }

            wareOrderTaskMapper.insert(wareOrderTask);

            List<WmsWareOrderTaskDetail> wareOrderTaskDetails = wareOrderTask.getDetails();
            for (WmsWareOrderTaskDetail wareOrderTaskDetail : wareOrderTaskDetails) {
                wareOrderTaskDetail.setTaskId(wareOrderTask.getId());
                wareOrderTaskDetailMapper.insert(wareOrderTaskDetail);
            }
            return wareOrderTask;

        }


        public void updateStatusWareOrderTaskByOrderId(String orderId,TaskStatus taskStatus){
            Example example=new Example(WmsWareOrderTask.class);
            example.createCriteria().andEqualTo("orderId",orderId);
            WmsWareOrderTask wareOrderTask=new WmsWareOrderTask();
            wareOrderTask.setTaskStatus(taskStatus);
            wareOrderTaskMapper.updateByExampleSelective(wareOrderTask,example);
        }


    /***
     * 库存锁定成功，准备出库，发送消息，由订单系统消费，修改状态为商品准备出库
     * @param wareOrderTask
     * @throws JMSException
     */
    public void sendSkuDeductMQ(WmsWareOrderTask wareOrderTask) throws JMSException{
        Connection conn = activeMQUtil.getConnection();

    Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
    Destination destination = session.createQueue("SKU_DEDUCT_QUEUE");
    MessageProducer producer = session.createProducer(destination);
    MapMessage mapMessage=new ActiveMQMapMessage();
        mapMessage.setString("orderId",wareOrderTask.getOrderId());
        mapMessage.setString("status",wareOrderTask.getTaskStatus().toString());
        producer.send(mapMessage);
        session.commit();
}

    @Transactional
    public void lockStock(WmsWareOrderTask wareOrderTask) {
        List<WmsWareOrderTaskDetail> wareOrderTaskDetails = wareOrderTask.getDetails();
        String comment = "";
        for (WmsWareOrderTaskDetail wareOrderTaskDetail : wareOrderTaskDetails) {

            WmsWareSku wareSku = new WmsWareSku();
            wareSku.setWarehouseId(wareOrderTask.getWareId());
            wareSku.setStockLocked(wareOrderTaskDetail.getSkuNum());
            wareSku.setSkuId(wareOrderTaskDetail.getSkuId());

            int availableStock = wareSkuMapper.selectStockBySkuidForUpdate(wareSku); //查询可用库存 加行级写锁 注意索引避免表锁
            if (availableStock - wareOrderTaskDetail.getSkuNum() < 0) {
                comment += "减库存异常：名称：" + wareOrderTaskDetail.getSkuName() + "，实际可用库存数" + availableStock + ",要求库存" + wareOrderTaskDetail.getSkuNum();
            }
        }

        if (comment.length() > 0) {   //库存超卖 记录日志，返回错误状态
            wareOrderTask.setTaskComment(comment);
            wareOrderTask.setTaskStatus(TaskStatus.OUT_OF_STOCK);
            updateStatusWareOrderTaskByOrderId(wareOrderTask.getOrderId(),TaskStatus.OUT_OF_STOCK);

        } else {   //库存正常  进行减库存
            for (WmsWareOrderTaskDetail wareOrderTaskDetail : wareOrderTaskDetails) {

                WmsWareSku wareSku = new WmsWareSku();
                wareSku.setWarehouseId(wareOrderTask.getWareId());
                wareSku.setStockLocked(wareOrderTaskDetail.getSkuNum());
                wareSku.setSkuId(wareOrderTaskDetail.getSkuId());

                wareSkuMapper.incrStockLocked(wareSku); //  加行级写锁 注意索引避免表锁

            }
            wareOrderTask.setTaskStatus(TaskStatus.DEDUCTED);
            updateStatusWareOrderTaskByOrderId(wareOrderTask.getOrderId(),TaskStatus.DEDUCTED);

        }

        try {
            sendSkuDeductMQ(wareOrderTask);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return;

    }



    public List<WmsWareOrderTask> getWareOrderTaskList(WmsWareOrderTask wareOrderTask){
             List<WmsWareOrderTask> wareOrderTasks=null;
            if(wareOrderTask==null){
                 wareOrderTasks = wareOrderTaskMapper.selectAll();
            }else{
                wareOrderTasks = wareOrderTaskMapper.select(wareOrderTask);
            }
            return wareOrderTasks;
    }

}
