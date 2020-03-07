package com.ky.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.*;
import com.ky.gmall.service.AttrService;
import com.ky.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import tk.mybatis.mapper.util.StringUtil;

import java.util.*;

@Controller
@CrossOrigin
public class SearchController {

    @Reference
    SearchService searchService;
    @Reference
    AttrService attrService;


    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap map) {
        //调用搜索服务,返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);
        map.put("skuLsInfoList", pmsSearchSkuInfoList);
        //将平台属性值集合放入set中(不重复)
        Set<String> valueIdSet = new HashSet<String>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                valueIdSet.add(pmsSkuAttrValue.getValueId());
            }
        }
        //根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);
        map.put("attrList", pmsBaseAttrInfos);

        //对平台属性集合进一步处理,去掉当前条件中valueId所在的属性组
        String[] delValueIDs = pmsSearchParam.getValueId();
        if (delValueIDs != null) {
            //面包屑
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            for (String delValueID : delValueIDs) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                //生成面包屑的参数
                pmsSearchCrumb.setValueId(delValueID);
                pmsSearchCrumb.setUrlParam(getUrlParam(pmsSearchParam, delValueID));

                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        if (delValueID.equals(valueId)) {
                            //查找面包屑的属性值名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            //删除该属性值所在的属性组
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            map.put("attrValueSelectedList", pmsSearchCrumbs);
        }

        String urlParam = getUrlParam(pmsSearchParam);
        map.put("urlParam", urlParam);
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            map.put("keyword", keyword);
        }

        return "list";
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam, String... delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (skuAttrValueList != null) {
            if (delValueId != null) {
                //如果delValueId有值,说明是面包屑
                String delValueIdStr = StringUtils.join(delValueId);
                for (String pmsSkuAttrValue : skuAttrValueList) {
                    if (!pmsSkuAttrValue.equals(delValueIdStr)) {
                        urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
                    }
                }
            } else {
                for (String pmsSkuAttrValue : skuAttrValueList) {
                    urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
                }
            }
        }
        return urlParam;
    }

    @RequestMapping("index")
    public String index() {

        return "index";
    }
}
