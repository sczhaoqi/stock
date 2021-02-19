package com.sczhaoqi.stock.tushare;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhaoqi
 * @date 2021/2/7 1:58 下午
 */
@ConditionalOnProperty(prefix = "tushare", name = "token")
@Configuration
@ConfigurationProperties(prefix = "tushare")
@Data
public class TushareProperties {
    private String token;
    private String address;
}
