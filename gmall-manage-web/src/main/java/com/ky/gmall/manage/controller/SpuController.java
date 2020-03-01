package com.ky.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.PmsBaseAttrInfo;
import com.ky.gmall.beans.PmsProductImage;
import com.ky.gmall.beans.PmsProductInfo;
import com.ky.gmall.beans.PmsProductSaleAttr;
import com.ky.gmall.manage.util.PmsUploadUtil;
import com.ky.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@CrossOrigin
public class SpuController {

    @Reference
    SpuService spuService;

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(String spuId){
        List<PmsProductImage> pmsProductImageList = spuService.spuImageList(spuId);
        return pmsProductImageList;
    }

    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){
        List<PmsProductSaleAttr> pmsProductSaleAttrList = spuService.spuSaleAttrList(spuId);
        return pmsProductSaleAttrList;
    }

    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile){
        //将图片的存储路径返回给页面
        String imgUrl = PmsUploadUtil.uploadImage(multipartFile);
        return imgUrl;
    }

    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){
        spuService.saveSpuInfo(pmsProductInfo);
        return "success";
    }

    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id){
        List<PmsProductInfo> pmsProductInfos = spuService.spuList(catalog3Id);
        return pmsProductInfos;
    }

}
