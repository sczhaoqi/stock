package com.sczhaoqi.stock.domain.tushare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhaoqi
 * @date 2021/2/18 4:29 下午
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockBasic {
    private String requestId;
    private Integer code;
    private String msg;
    private TushareData data;
    private Boolean hasMore;


}
