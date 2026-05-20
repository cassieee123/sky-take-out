package com.sky.mapper;


import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    //根据菜品id查询对应的套餐id
    List<Long> getSetmealIdsByDishIds(List<Long> ids);

    //保存新增的套餐和菜品的关系
    void insertBatch(List<SetmealDish> setmealDishes);

    //删除套餐和菜品之间的关系
    void delete(Long id);

    //批量删除套餐和菜品之间的关系
    void deleteBatch(List<Long> ids);

    //根据套餐id查询当前套餐和菜品的关联情况
    List<SetmealDish> getBySetmealId(Long id);


}
