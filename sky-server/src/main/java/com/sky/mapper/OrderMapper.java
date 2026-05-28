package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {

    /**
     * 新增订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据条件查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> list(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单详情
     * @param id
     * @return
     */
    OrderVO orderDetail(Long id);

    /**
     * 对订单进行条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据状态对订单数量统计
     * @param orders
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer statistics(Orders orders);

    /**
     * 根据订单id来查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);
}
