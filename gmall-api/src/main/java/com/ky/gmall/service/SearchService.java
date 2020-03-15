package com.ky.gmall.service;

import com.ky.gmall.beans.PmsSearchParam;
import com.ky.gmall.beans.PmsSearchSkuInfo;
import com.ky.gmall.beans.PmsSkuInfo;

import java.util.List;

public interface SearchService {
    List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam);

}
