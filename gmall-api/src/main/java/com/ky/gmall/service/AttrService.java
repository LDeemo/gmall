package com.ky.gmall.service;

import com.ky.gmall.beans.PmsBaseAttrInfo;
import com.ky.gmall.beans.PmsBaseAttrValue;
import com.ky.gmall.beans.PmsBaseSaleAttr;

import java.util.List;

public interface AttrService {
    List<PmsBaseAttrInfo> attrInfoList(String catalog3Id);

    String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);

    List<PmsBaseAttrValue> getAttrValueList(String attrId);

    List<PmsBaseSaleAttr> baseSaleAttrList();
}
