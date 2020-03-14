package com.ky.gware.gware.mapper;

import com.ky.gware.gware.bean.WmsWareSku;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @param
 * @return
 */
public interface WareSkuMapper extends Mapper<WmsWareSku> {


    public Integer selectStockBySkuid(String skuid);

    public int incrStockLocked(WmsWareSku wareSku);

    public int selectStockBySkuidForUpdate(WmsWareSku wareSku);

    public int  deliveryStock(WmsWareSku wareSku);

    public List<WmsWareSku> selectWareSkuAll();
}
