package com.sczhaoqi.stock.tushare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sczhaoqi.stock.base.ApiClient;
import com.sczhaoqi.stock.domain.tushare.CodeInfo;
import com.sczhaoqi.stock.domain.tushare.StockBasic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhaoqi
 * @date 2021/2/7 1:53 下午
 */

@ConditionalOnProperty(prefix = "tushare", name = "token")
@Qualifier(value = "tuShareClient")
@Component
@Slf4j
public class TuShareClient implements ApiClient {

    private final TushareProperties prop;

    private final RestTemplate restTemplate;

    private final MongoTemplate mongoTemplate;

    private final DateTimeFormatter dateTimeFormatter;
    private ObjectMapper mapper;

    public TuShareClient(TushareProperties properties, MongoTemplate mongoTemplate) {
        this.prop = properties;
        this.restTemplate = new RestTemplate();
        this.mongoTemplate = mongoTemplate;
        this.mapper = new ObjectMapper();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    }

    @Override
    public void init() {

    }

    @Override
    @PostConstruct
    public void test() {
        loadStockBasic();
        loadTradeCal();
        loadDailyData();
    }

    /**
     * 获取基础信息数据，包括股票代码、名称、上市日期、退市日期等
     */
    private void loadStockBasic() {
        Map<String, Object> params = new HashMap<>();
        params.put("list_status", "L");
        ResponseEntity<StockBasic> res = restTemplate.postForEntity(prop.getAddress(), new TushareRequest("stock_basic", prop.getToken(), params), StockBasic.class);

        if (res.getStatusCodeValue() != HttpStatus.OK.value()) {
            throw new RuntimeException("请求失败");
        }
        StockBasic body = res.getBody();
        String[] fields = body.getData().getFields();
        List<Map<String, Object>> data = Arrays.stream(body.getData().getItems()).map(it -> {
            Map<String, Object> id = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                id.put(fields[i], it[i]);
            }
            return id;
        }).collect(Collectors.toList());

        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, "stock_basic");
        data.forEach(dd -> {
            Update u1 = new Update();
            dd.entrySet().forEach(kv -> {
                u1.set(kv.getKey(), kv.getValue());
            });
            Query q1 = new Query(Criteria.where("ts_code").is(dd.get("ts_code")).and("list_date").is(dd.get("list_date")));
            ops.upsert(q1, u1);
        });

        ops.execute();
    }

    /**
     * 获取各大交易所交易日历数据,默认提取的是上交所
     */
    private void loadTradeCal() {
        Map<String, Object> params = new HashMap<>();
        // 交易所 SSE上交所,SZSE深交所,CFFEX 中金所,SHFE 上期所,CZCE 郑商所,DCE 大商所,INE 上能源
//        params.put("exchange", "L");
        params.put("start_date", "19000101");
        params.put("end_date", dateTimeFormatter.format(LocalDate.now()));
//        params.put("is_open", "1");
        ResponseEntity<StockBasic> res = restTemplate.postForEntity(prop.getAddress(), new TushareRequest("trade_cal", prop.getToken(), params), StockBasic.class);

        if (res.getStatusCodeValue() != HttpStatus.OK.value()) {
            throw new RuntimeException("请求失败");
        }
        StockBasic body = res.getBody();
        String[] fields = body.getData().getFields();
        List<Map<String, Object>> data = Arrays.stream(body.getData().getItems()).map(it -> {
            Map<String, Object> id = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                id.put(fields[i], it[i]);
            }
            return id;
        }).collect(Collectors.toList());

        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, "trade_cal");
        data.forEach(dd -> {
            Update u1 = new Update();
            dd.entrySet().forEach(kv -> {
                u1.set(kv.getKey(), kv.getValue());
            });
            Query q1 = new Query(Criteria.where("exchange").is(dd.get("exchange")).and("cal_date").is(dd.get("cal_date")));
            ops.upsert(q1, u1);
        });

        ops.execute();
    }

    private List<CodeInfo> getCodeInfo() {
        return mongoTemplate.findAll(CodeInfo.class, "stock_basic");
    }

    /**
     * 获取历史股票日数据
     */
    private void loadDailyData() {
        boolean inited = mongoTemplate.collectionExists("daily");
        List<CodeInfo> codes = getCodeInfo();
        if (!inited) {
            codes.forEach(code -> {
                String sDate = code.getList_date();
                LocalDate sDay = LocalDate.parse(sDate, dateTimeFormatter);
                while (sDay.isBefore(LocalDate.now())) {
                    log.info("load {}", code.getTs_code());
                    loadDaily(code.getTs_code(), sDay.format(dateTimeFormatter), sDay.plusDays(4000).format(dateTimeFormatter));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sDay = sDay.plusDays(4000);
                }
            });
        } else {
            String tcs = "";
            for (int i = 0; i < codes.size(); i++) {
                tcs += codes.get(i).getTs_code();
                if ((i + 1) % 1000 == 0) {
                    loadDaily(new String(tcs), LocalDate.now().minusDays(1).format(dateTimeFormatter), LocalDate.now().format(dateTimeFormatter));
                    tcs = "";
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!"".equals(tcs)) {
                loadDaily(new String(tcs), LocalDate.now().minusDays(1).format(dateTimeFormatter), LocalDate.now().format(dateTimeFormatter));
            }
        }
    }

    /**
     * 日线行情
     *
     * @param tsCode
     * @param start
     * @param end
     */

    private void loadDaily(String tsCode, String start, String end) {
        Map<String, Object> params = new HashMap<>();
        // 交易所 SSE上交所,SZSE深交所,CFFEX 中金所,SHFE 上期所,CZCE 郑商所,DCE 大商所,INE 上能源
        params.put("ts_code", tsCode);
        params.put("start_date", start);
        params.put("end_date", end);
//        params.put("trade_date", "19000101");
        ResponseEntity<StockBasic> res = restTemplate.postForEntity(prop.getAddress(), new TushareRequest("daily", prop.getToken(), params), StockBasic.class);

        if (res.getStatusCodeValue() != HttpStatus.OK.value()) {
            throw new RuntimeException("请求失败");
        }
        StockBasic body = res.getBody();
        String[] fields = body.getData().getFields();
        List<Map<String, Object>> data = Arrays.stream(body.getData().getItems()).map(it -> {
            Map<String, Object> id = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                id.put(fields[i], it[i]);
            }
            return id;
        }).collect(Collectors.toList());
        mongoTemplate.insert(data, "daily");
    }
}
