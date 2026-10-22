package cn.bitoffer.seckill.controller;

import cn.bitoffer.seckill.common.ResponseEntity;
import cn.bitoffer.seckill.common.ResponseEnum;
import cn.bitoffer.seckill.model.Goods;
import cn.bitoffer.seckill.service.SecKillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/sec_kill")
@Slf4j
public class SecKillController {

    @Autowired
    private SecKillService secKillService;

    /**
     * 获取商品信息接口
     * 该接口通过商品编号获取商品详细信息，并以HTTP响应的形式返回
     *
     * @param traceID 请求跟踪ID，用于跟踪请求流程
     * @param userID 用户ID，用于标识请求用户
     * @param goodsNum 商品编号，用于查询商品信息
     * @return ResponseEntity<GetGoodsInfoData> 包含商品信息的响应实体
     */
    @GetMapping("/get_goods_info")
    public ResponseEntity<GetGoodsInfoData> getGoodsInfo(@RequestHeader("Trace-ID") String traceID,
                                                         @RequestHeader("User-ID") Long userID,
                                                         @RequestParam(name = "goodsNum") String goodsNum) {
        // 通过服务层方法获取商品信息
        Goods goods = secKillService.getGoodsByNum(traceID, userID, goodsNum);

        // 创建响应数据对象
        GetGoodsInfoData data = new GetGoodsInfoData();
        // 初始化商品信息对象
        data.goodsInfo = new GoodsInfo();

        // 将商品信息填充到响应对象中
        data.goodsInfo.goodsNum = goods.getGoodsNum();
        data.goodsInfo.goodsName = goods.getGoodsName();
        data.goodsInfo.price = goods.getPrice();
        data.goodsInfo.picUrl = goods.getPicUrl();
        data.goodsInfo.seller = goods.getSeller();
        System.out.println(data);
        return ResponseEntity.resp(ResponseEnum.OK, data);
    }

    // 获取商品列表的API接口
    @GetMapping(value ="/get_goods_list")
    public ResponseEntity<GetGoodsListData> getGoodsList(@RequestHeader("Trace-ID") String traceID,
                                                         @RequestHeader("User-ID") Long userID,
                                                         @RequestParam(name = "offset") Integer offset,
                                                         @RequestParam(name = "limit") Integer limit) {
        // 如果请求的商品数量限制为0，则默认为200
        if (limit == 0) {
            limit = 200;
        }
        // 调用服务层方法获取商品列表
        ArrayList<Goods> goodsList = secKillService.getGoodsList(traceID, userID, offset, limit);
        // 初始化商品信息列表
        GetGoodsListData data = new GetGoodsListData();
        // 初始化商品信息列表
        data.goodsList = new ArrayList<GoodsInfo>();
        // 遍历商品列表，转换并填充商品信息
        for (int i = 0; i < goodsList.size(); i++) {
            GoodsInfo goodsInfo = new GoodsInfo();
            goodsInfo.goodsNum = goodsList.get(i).getGoodsNum();
            goodsInfo.goodsName = goodsList.get(i).getGoodsName();
            goodsInfo.price = goodsList.get(i).getPrice();
            goodsInfo.picUrl = goodsList.get(i).getPicUrl();
            goodsInfo.seller = goodsList.get(i).getSeller();
            data.goodsList.add(goodsInfo);
        }
        System.out.println(data);
        return ResponseEntity.resp(ResponseEnum.OK, data);
    }
    @PostMapping(value = "/v1/sec_kill", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> secKillV1(@RequestHeader("Trace-ID") String traceID,
                                            @RequestHeader("User-ID") Long userID,
                                            @RequestBody SecKillV1Req data) {
        String orderNum = "";
        //try {
        //    String goodsNum = data.goodsNum;
        //    Integer num = data.num;
        //    orderNum = secKillService.secKillV1(traceID, userID, goodsNum, num);
        //    System.out.println(orderNum);
        //} catch (Exception e) {
        //    System.out.println("seckill err " + e.getMessage());
        //}
        String goodsNum = data.goodsNum;
        Integer num = data.num;
        orderNum = secKillService.secKillV1(traceID, userID, goodsNum, num);
        System.out.println(orderNum);
        return ResponseEntity.ok(orderNum);
    }

    //处理秒杀V2请求的API接口
    @PostMapping(value = "/v2/sec_kill", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> secKillV2(@RequestHeader("Trace-ID") String traceID,
                                            @RequestHeader("User-ID") Long userID,
                                            @RequestBody SecKillV1Req data) {
        //初始化订单号未空字符串
        String orderNum = "";
        try {
            // 提取请求中的商品编号和购买数量
            String goodsNum = data.goodsNum;
            Integer num = data.num;

            // 调用服务层方法执行秒杀逻辑，生成订单号
            orderNum = secKillService.secKillV2(traceID, userID, goodsNum, num);
            System.out.println(orderNum);
        } catch (Exception e) {
            System.out.println("seckill err " + e.getMessage());
        }
        return ResponseEntity.ok(orderNum);
    }

    @PostMapping(value = "/v3/sec_kill", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> secKillV3(@RequestHeader("Trace-ID") String traceID,
                                            @RequestHeader("User-ID") Long userID,
                                            @RequestBody SecKillV1Req data) {
        String secNum = "";
        try {
            String goodsNum = data.goodsNum;
            Integer num = data.num;
            secNum = secKillService.secKillV3(traceID, userID, goodsNum, num);
            System.out.println(secNum);
        } catch (Exception e) {
            System.out.println("seckill err " + e.getMessage());
        }
        return ResponseEntity.ok(secNum);
    }

    public static class SecKillV1Req {
        public String goodsNum;
        public Integer num;
    }



    public static class GetGoodsInfoData {
        public GoodsInfo goodsInfo;
    }

    public static class GetGoodsListData {
        public List<GoodsInfo> goodsList;
    }



    public static class GoodsInfo{
        public String goodsNum;
        public String goodsName;
        public Float price;
        public String picUrl;
        public Long seller;
    }
}