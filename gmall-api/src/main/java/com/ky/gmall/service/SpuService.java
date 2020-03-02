package com.ky.gmall.service;

import com.ky.gmall.beans.PmsBaseAttrInfo;
import com.ky.gmall.beans.PmsProductImage;
import com.ky.gmall.beans.PmsProductInfo;
import com.ky.gmall.beans.PmsProductSaleAttr;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);

    void saveSpuInfo(PmsProductInfo pmsProductInfo);

    List<PmsProductSaleAttr> spuSaleAttrList(String spuId);

    List<PmsProductImage> spuImageList(String spuId);

    List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId,String skuId);
}
