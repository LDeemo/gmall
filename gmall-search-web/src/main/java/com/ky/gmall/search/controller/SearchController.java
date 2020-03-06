package com.ky.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.PmsSearchParam;
import com.ky.gmall.beans.PmsSearchSkuInfo;
import com.ky.gmall.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@CrossOrigin
public class SearchController {

    @Reference
    SearchService searchService;


    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap map){
        //调用搜索服务,返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);

        map.put("skuLsInfoList",pmsSearchSkuInfoList);


        return "list";
    }

    @RequestMapping("index")
    public String index(){

        return "index";
    }
}
