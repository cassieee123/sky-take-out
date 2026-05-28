package com.sky.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private ShoppingCartService shoppingCartService;
    /**
     * 提交订单  用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //1、处理各种异常（地址簿为空、购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.size() == 0){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //检查用户的收货地址是否超出配送范围
        checkOutRange(addressBook.getProvinceName() + addressBook.getDistrictName() + addressBook.getDetail());

        //2、向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);//当前支付状态
        orders.setStatus(Orders.PENDING_PAYMENT);//当前订单状态
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//订单号  使用时间戳作为订单号
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(userId);//用户id

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3、向订单明细表中插入n条数据
        for(ShoppingCart cart: shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);//批量插入订单明细数据

        //4、清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5、封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 对历史订单进行分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        //设置分页参数
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //调用Mapper
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<OrderVO> page = orderMapper.list(ordersPageQueryDTO);
        //返回PageResult
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 查看订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        return orderMapper.orderDetail(id);
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancelOrder(Long id) {
        Orders orders = Orders.builder().status(Orders.CANCELLED).id(id).cancelTime(LocalDateTime.now()).build();
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //1、根据订单id查询订单详情数据
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
        if(orderDetailList == null || orderDetailList.size() == 0){//若是该订单没有订单明细 返回异常信息
            throw new OrderBusinessException(MessageConstant.ORDER_DETAIL_IS_NULL);
        }

        //2、将订单详情数据转换为购物车数据，插入购物车表
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for(OrderDetail orderDetail: orderDetailList){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 对订单进行条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //设置分页参数
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //调用Mapper
//        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());//这个地方不需要  因为我们就是要不同用户的订单查询 所以不需要限制用户
        Page<OrderVO> page = orderMapper.conditionSearch(ordersPageQueryDTO);
        //返回PageResult
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 对各个状态订单的数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statisticOrder() {
        //这个地方是可以设置一个函数，就是将orders传入进去，只设置status信息，因为不然的话需要很多个Mapper，后续要是状态修改的话很不好修改

        Orders orders = new Orders();
        orders.setStatus(Orders.CONFIRMED);//这是已接单状态的订单
        Integer confirmNum = orderMapper.statistics(orders);

        orders.setStatus(Orders.TO_BE_CONFIRMED);//这是待接单状态的订单
        Integer toBeConfirmNum = orderMapper.statistics(orders);

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);//这是派送中状态的订单
        Integer deliverInProgressNum = orderMapper.statistics(orders);

        OrderStatisticsVO orderStatisticsVO = OrderStatisticsVO.builder().confirmed(confirmNum).toBeConfirmed(toBeConfirmNum)
                .deliveryInProgress(deliverInProgressNum).build();
        return orderStatisticsVO;
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //根据订单id查询订单信息
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //只有待付款、待接单、已接单的订单才能取消
        if(ordersDB.getStatus() != Orders.PENDING_PAYMENT && ordersDB.getStatus() != Orders.TO_BE_CONFIRMED && ordersDB.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为已取消，设置取消时间和取消备注
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelTime(LocalDateTime.now())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .build();//设置取消原因  取消时间  状态

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        //根据订单id查询订单信息
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //只有派送中状态的订单才能完成
        if(ordersDB.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为已完成，设置完成时间
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();//设置状态和送达时间

        orderMapper.update(orders);
    }

    /**
     * 拒绝订单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //根据订单id查询订单信息
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //只有待接单状态的订单才能拒绝
        if(ordersDB.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为已取消，设置和拒绝原因
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .build();//设置拒绝原因  状态

        orderMapper.update(orders);
    }

    /**
     * 接单  确认订单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //根据订单id查询订单信息
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //只有待接单状态的订单才能接单
        if(ordersDB.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为已接单，设置
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();//设置状态

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        //根据订单id查询订单信息
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //只有待派送的订单才能派送
        if(ordersDB.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为派送中，设置派送时间
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(orders);
    }


    //下面是提供一个校验方法
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 检查客户的收获地址是否超出配送范围
     * @param address  客户的收获地址
     */
    public void checkOutRange(String address){
        HashMap map = new HashMap();
        map.put("address", shopAddress);//将店铺地址放进去
        map.put("output", "json");//输出形式是JSON
        map.put("ak", ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("http://api.map.baidu.com/geocoding/v3", map);//调用地址编码服务
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");//获取维度坐标
        String lng = location.getString("lng");//获取经度坐标
        //店铺经纬度坐标
        String shopLngLat = lng + "," + lat;

        map.put("address", address);//将配送地址放进去
        //获取瘦多地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("http://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收获地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //收获地址经纬度坐标
        String userLngLat = lng + "," + lat;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info", "0");

        //下面是进行骑行路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/riding", map);
        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("骑行路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");//取出result【是一个小JSON对象】
        JSONArray jsonArray =(JSONArray) result.get("routes");//从result中取出routes【是一个JSONArray数组】
        Integer distance = (Integer) ((JSONObject)jsonArray.get(0)).get("distance");
        //从routes数组中取出第一个元素，先转成JSON对象，再取出distance距离信息

        if(distance > 5000){
            throw new OrderBusinessException("超出配送范围");
        }
    }
}





