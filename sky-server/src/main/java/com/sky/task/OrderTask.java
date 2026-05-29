package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ? ")//每分钟触发一次
//    @Scheduled(cron = "0/5 * * * * ?")测试用
    public void processTimeOutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);//这里也是距离下单时间 超出了15分钟 然后每分钟进行判定

        // select * from orders where status  =  ?paisongzhong  and order_time < (local_time - 15)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if(ordersList != null && ordersList.size() > 0){
            ordersList.forEach(orders -> {
                orders.setStatus(orders.CANCELLED);//更新状态
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }
    }

    @Scheduled(cron = "0 0 1 * * ? ")//每天凌晨一点触发一次
//    @Scheduled(cron = "1/5 * * * * ?")测试用
    public void processDeliveryOrder(){//对于这类  需要判断是否一直处于派送中
        log.info("定时处理处于派送中的订单{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);//下单要是超出一小时  然后在凌晨一点统一判定
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if(ordersList != null && ordersList.size() > 0){
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);//这其实就肯定已经送达了  直接设置送达完成
                orderMapper.update(orders);
            });
        }

    }
}
