package com.sky.mapper;


import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface UserMapper {

    //根据openid查找User
    @Select("select * from user where openid = #{openid}")
    public User getByOpenid(String openid);

    //插入数据
    public void insert(User user);

    //根据ID
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 根据条件动态统计用户数量
     * @param map
     * @return
     */
    Integer countUserByMap(Map map);



}
