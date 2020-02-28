package com.ky.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.ky.gmall.beans.PmsBaseAttrInfo;
import com.ky.gmall.beans.PmsProductInfo;
import com.ky.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.ky.gmall.manage.mapper.PmsProductInfoMapper;
import com.ky.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;

    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {
        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> pmsProductInfos = pmsProductInfoMapper.select(pmsProductInfo);
        return pmsProductInfos;
    }
}
