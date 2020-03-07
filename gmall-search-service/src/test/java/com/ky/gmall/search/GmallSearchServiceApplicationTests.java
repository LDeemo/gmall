package com.ky.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.PmsSearchSkuInfo;
import com.ky.gmall.beans.PmsSkuAttrValue;
import com.ky.gmall.beans.PmsSkuInfo;
import com.ky.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService; //查询mysql数据


    @Test
    void contextLoads() throws IOException, InvocationTargetException, IllegalAccessException {
        put();
    }

    public void put() throws IOException, InvocationTargetException, IllegalAccessException {

        JestClientFactory factory = new JestClientFactory();

        String connectionUrl = "http://47.104.172.91:9200";

        factory.setHttpClientConfig(new HttpClientConfig.Builder(connectionUrl).multiThreaded(true).connTimeout(60000).readTimeout(60000).build());
        JestClient jestClient = factory.getObject();


        //查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = skuService.getAllSku("61");

        //转化为es数据结构
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(pmsSearchSkuInfo,pmsSkuInfo);

            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }

        //存入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()).build();
            jestClient.execute(put);
        }



    }
}
