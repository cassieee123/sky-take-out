package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    //批量插入订单明细数据
    public void insertBatch(List<OrderDetail> orderDetailList);

    //根据订单id查询订单明细数据
    List<OrderDetail> listByOrderId(Long id);
}
