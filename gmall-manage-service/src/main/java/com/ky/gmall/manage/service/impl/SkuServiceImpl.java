package com.ky.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.ky.gmall.beans.PmsSkuAttrValue;
import com.ky.gmall.beans.PmsSkuImage;
import com.ky.gmall.beans.PmsSkuInfo;
import com.ky.gmall.beans.PmsSkuSaleAttrValue;
import com.ky.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.ky.gmall.manage.mapper.PmsSkuImageMapper;
import com.ky.gmall.manage.mapper.PmsSkuInfoMapper;
import com.ky.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.ky.gmall.service.SkuService;
import com.ky.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo pmsSkuInfo = null;
        //连接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);

        if (StringUtils.isNotBlank(skuJson)){
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            //如果缓存中没有,查询mysql

            //设置分布式锁
            String OK = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10);
            if (StringUtils.isNotBlank(OK)&&"OK".equals(OK)){
                //设置成功,有权在10秒的过期时间内访问数据库
                pmsSkuInfo = getSkuByIdFromDb(skuId);

                if (pmsSkuInfo!=null){
                    //mysql查询结果存入redis
                    jedis.set(skuKey,JSON.toJSONString(pmsSkuInfo));
                }else {
                    //数据库中不存在该sku
                    //为了防止缓存穿透,将null值或空字符串设置给redis
                    jedis.setex(skuKey,60*3,JSON.toJSONString(""));

                }
            }else {
                //设置失败,自旋(该线程在睡眠几秒后重新尝试访问)
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return getSkuById(skuId);
            }

        }

        //释放连接
        jedis.close();
        return pmsSkuInfo;
    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId){
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //sku图片集合
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImages);
        return skuInfo;
    }



    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        //插入skuInfo
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();
        //插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }
        //插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
        //插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }

    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }
}
