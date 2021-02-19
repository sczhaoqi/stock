package com.sczhaoqi.stock.domain.tushare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhaoqi
 * @date 2021/2/18 4:31 下午
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TushareData {
    private String[] fields;
    private Object[][] items;
}
