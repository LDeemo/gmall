package com.ky.gmall.service;

import com.ky.gmall.beans.PmsProductInfo;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);
}
