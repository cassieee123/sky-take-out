package com.sky.mapper;


import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    //根据openid查找User
    @Select("select * from user where openid = #{openid}")
    public User getByOpenid(String openid);

    //插入数据
    public void insert(User user);
}
