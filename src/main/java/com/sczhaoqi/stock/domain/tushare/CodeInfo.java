package com.sczhaoqi.stock.domain.tushare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhaoqi
 * @date 2021/2/18 9:54 下午
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeInfo {
    private String ts_code;
    private String list_date;
    private String area;
    private String id;
    private String name;
}
