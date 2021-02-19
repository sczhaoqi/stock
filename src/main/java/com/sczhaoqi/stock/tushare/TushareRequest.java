package com.sczhaoqi.stock.tushare;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhaoqi
 * @date 2021/2/7 2:14 下午
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TushareRequest {
    @JsonProperty("api_name")
    private String apiName;
    private String token;
    private Object params;
    private String fields = "";

    public TushareRequest(String apiName, String token, Object params) {
        this.apiName = apiName;
        this.token = token;
        this.params = params;
        this.fields = "";
    }
}
