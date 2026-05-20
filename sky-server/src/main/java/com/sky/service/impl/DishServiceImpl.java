package com.sky.service.impl;



import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    public DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品
     * @param dishDTO
     */
    @Override
    @Transactional()
    public void saveWithFlavor(DishDTO dishDTO) {
        //引入一个新类
        Dish dish = new Dish();
        //然后复制新类的所有的内容
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入一条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {//这个地方对象可以存在  且里面为空，所以需要进行两次判断，第一次判断是  对象存在，第二次判断是对象中内容不为空
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /*
    分页查询
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {//DTO将想存入的页码和记录数都存在了
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        long total = page.getTotal();
        List<DishVO> result = page.getResult();
        return new PageResult(total, result);
    }

    @Override
    @Transactional()
    //被它标记的方法/类，所有数据库操作会被包裹在同一个事务中。
    // 意思就是说将方法交给Spring进行事务管理，方法执行前，开启事务。方法执行完毕，提交事务。
    public void deleteBatch(List<Long> ids) {
        //不能删除  存在起售中的菜品
        for(Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()==StatusConstant.ENABLE){//表示当前在起售中
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //不能删除：菜品被套餐关联
        //这个函数是给你菜品列表，然后最终返回套餐列表
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size()>0){
            //当前套餐被关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品后，关联的口味数据也需要删除
        //法一：可以自定义循环，然后在循环中依次删除菜品和口味数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //删除口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        //法二：批量删除口味数据  套餐也是可以的
        dishMapper.deleteByIds(ids);//删除菜品数据
        dishFlavorMapper.deleteByDishIds(ids);//删除口味数据
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据ID查询菜品数据
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味信息
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //将查询的到数据封装到VO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);//将菜品信息放到VO中去
        dishVO.setFlavors(dishFlavors);//将菜品口味信息放到VO中去
        return dishVO;
    }

    @Override
    //根据id 修改菜品的基本信息和对应的口味信息
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品表基本信息
        dishMapper.update(dish);
        //删除原有的口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();//获取新口味数据
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
        }
        dishFlavorMapper.insertBatch(flavors);
    }

    //起售  停售商品
    //首先先创建一个新的dish  然后进行更新就好，跟上面的update用一个
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();

        dishMapper.update(dish);
    }

    //根据分类id查询菜品

    @Override
    public List<Dish> listByCategoryId(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
            return dishMapper.list(dish);
    }
}
