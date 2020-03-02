package com.ky.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.PmsProductSaleAttr;
import com.ky.gmall.beans.PmsSkuInfo;
import com.ky.gmall.service.SkuService;
import com.ky.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@CrossOrigin
public class ItemController {

    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId,ModelMap map){
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);

        //sku对象
        map.put("skuInfo",pmsSkuInfo);
        //销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),skuId);
        map.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);
        return "item";
    }



    @RequestMapping("index")
    public String index(ModelMap modelMap){
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add("循环数据"+i);
        }
        modelMap.put("check","1");
        modelMap.put("list",list);
        modelMap.put("salary","15k");
        modelMap.put("hello","hello thymeleaf !!");
        return "index";
    }
}
