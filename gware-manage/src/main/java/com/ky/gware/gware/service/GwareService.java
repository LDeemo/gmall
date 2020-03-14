package com.ky.gware.gware.service;


import com.ky.gware.gware.bean.WmsWareInfo;
import com.ky.gware.gware.bean.WmsWareOrderTask;
import com.ky.gware.gware.bean.WmsWareSku;

import java.util.List;
import java.util.Map;

/**
 * @param
 * @return
 */
public interface GwareService {
    public Integer  getStockBySkuId(String skuid);

    public boolean  hasStockBySkuId(String skuid, Integer num);

    public List<WmsWareInfo> getWareInfoBySkuid(String skuid);

    public void addWareInfo();

    public Map<String,List<String>> getWareSkuMap(List<String> skuIdlist);

    public void addWareSku(WmsWareSku wareSku);

    public void deliveryStock(WmsWareOrderTask taskExample) ;

    public WmsWareOrderTask saveWareOrderTask(WmsWareOrderTask wareOrderTask);

    public  List<WmsWareOrderTask>   checkOrderSplit(WmsWareOrderTask wareOrderTask);

    public void lockStock(WmsWareOrderTask wareOrderTask);

    public List<WmsWareOrderTask> getWareOrderTaskList(WmsWareOrderTask wareOrderTask);

    public List<WmsWareSku> getWareSkuList();

    public List<WmsWareInfo> getWareInfoList();
}
