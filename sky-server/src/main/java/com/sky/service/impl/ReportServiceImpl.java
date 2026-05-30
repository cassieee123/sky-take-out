package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    /**
     * 统计指定时间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合存放从begin到end范围内的每天日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!(begin.equals(end))) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();//存放每天的营业额数据
        dateList.forEach(date->{//有几天查几次
            //查询date日期对应的营业额数据，营业是指：状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//当前一天的最开始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(count) from order where order_time > ? and order_time < ? and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);//这个地方根据动态条件去数据库查数据，必须用一个对象把所有条件装在一起，才能传给SQL
            //考虑到当前营业额为0的情况，会返回空
            turnover = turnover == null ? 0.0 : turnover;

            turnoverList.add(turnover);
        });


        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //查询begin到end范围内每天新增用户数量
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!(begin.equals(end))) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();//存放每天的新增用户数量
        List<Integer> totalUserList = new ArrayList<>();//存放每天的总用户数量

        dateList.forEach(date->{//有几天查几次
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//当前一天的最开始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select count(*) from user where create_time > ? and create_time < ?
            Map map = new HashMap();
            map.put("end", endTime);
            Integer totalUser = userMapper.countUserByMap(map);
            map.put("begin", beginTime);
            Integer newUser = userMapper.countUserByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        });

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //查询begin到end范围内每天订单数量和有效订单数量
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!(begin.equals(end))) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();//存放每天的订单数量
        List<Integer> validOrderCountList = new ArrayList<>();//存放每天的有效订单数量

        dateList.forEach(date->{//有几天查几次
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//当前一天的最开始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select count(*) from order where order_time > ? and order_time < ?
            //查询每天的订单数
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天的有效订单数
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        });

        //计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        //计算时间区间内有效的订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);
        //计算订单完成率
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (validOrderCount * 100.0 / totalOrderCount);

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    //根据条件统计订单数量【感觉这种里面  复用达到两个及以上就抽出来了】
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 销量排名前十名统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> top10 = orderMapper.getTop10(beginTime, endTime);

        //因为VO中用的是String
        List<String> names = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");
        return SalesTop10ReportVO.builder().nameList(nameList).numberList(numberList).build();
    }
}
