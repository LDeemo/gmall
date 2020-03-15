package com.ky.gmall.search.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.ky.gmall.beans.PmsSearchSkuInfo;
import com.ky.gmall.beans.PmsSkuInfo;
import com.ky.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SkuServiceMqListener {

    @Reference
    SkuService skuService;

    @JmsListener(destination = "SKU_SAVE_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        if (StringUtils.isBlank(mapMessage.getString("pmsSkuInfo"))) {
            return;
        }
        PmsSkuInfo pmsSkuInfoAdd = JSON.parseObject(mapMessage.getString("pmsSkuInfo"), PmsSkuInfo.class);
        String catalog3Id = pmsSkuInfoAdd.getCatalog3Id();
        JestClientFactory factory = new JestClientFactory();

        String connectionUrl = "http://47.104.172.91:9200";

        factory.setHttpClientConfig(new HttpClientConfig.Builder(connectionUrl).multiThreaded(true).connTimeout(60000).readTimeout(60000).build());
        JestClient jestClient = factory.getObject();


        //查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = skuService.getAllSku(catalog3Id);

        if (pmsSkuInfoList != null && pmsSkuInfoList.size() > 0) {
            //转化为es数据结构
            List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

            for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
                PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
                try {
                    BeanUtils.copyProperties(pmsSearchSkuInfo, pmsSkuInfo);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                pmsSearchSkuInfos.add(pmsSearchSkuInfo);
            }

            //存入es
            for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
                Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()).build();
                try {
                    jestClient.execute(put);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
