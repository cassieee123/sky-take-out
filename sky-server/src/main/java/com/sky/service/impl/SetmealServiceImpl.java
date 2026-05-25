package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    //新增套餐
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表中插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long id = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(id);//设置的是套餐id，前端传过来的时候，套餐id根本不存在，是null。菜品id是知道的
            //所以我将setmealDTO中setmealDishes的每个套餐id设置为刚知道的套餐id数据
        });
        //保存套餐和菜品的关系
        setmealDishMapper.insertBatch(setmealDishes);//现在套餐id也已经知道了，所以将所有的菜品id 和这个套餐id联系起来  新增这个表的数据
    }

    //分页查询
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        //意思是开启分页
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());//封装成统一的
    }

    //批量删除套餐
    @Override
    public void deletBatch(List<Long> ids) {
        //起售中的套餐不能删除
        ids.forEach(id -> {
           SetmealVO setmealVO = setmealMapper.getById(id);
           if (setmealVO.getStatus()== StatusConstant.ENABLE){//说明当前套餐还在起售中
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
           }
        });

//        ids.forEach(id -> {
//            //下面开始套餐信息删除
//            setmealMapper.delete(id);
//            //下面开始删除套餐和菜品信息
//            setmealDishMapper.delete(id);
//        });

        //下面是批量删除，第二种方式
        setmealMapper.deleteBatch(ids);
        setmealDishMapper.deleteBatch(ids);
    }

    //根据ID查询套餐
    @Override
    public SetmealVO getById(Long id) {
        //先查询到当前套餐信息
        SetmealVO setmealVO = setmealMapper.getById(id);

        //下面查询一下套餐和菜品的关联关系
        //现在已经有套餐id了，找关联的套餐 菜品表
        List<SetmealDish>setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    //对套餐进行起售或停售
    @Override
    public void startOrStop(Integer status, Long id) {
        if(status==StatusConstant.ENABLE){//要是当前套餐是起售
            //根据套餐id查询，当前菜品是否有停售的，要是有停售，提示“套餐内包含未起售菜品，无法起售”
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList.size()>0 && dishList != null){
                dishList.forEach(dish -> {
                    if(dish.getStatus() == StatusConstant.DISABLE){//要是心在菜品有未起售的 就抛出一个异常
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        //这个地方改进一下，下面就是操作数据库改一下状态
        //创造者模式
        Setmeal setmeal = Setmeal.builder()
                        .id(id).status(status).build();
        setmealMapper.update(setmeal);
    }

    //修改套餐
    //我知道了，这个地方确实是需要获取套餐  然后删除原来关联数据 再重新加的 是因为套餐跟菜品之间是会改的  可能会这个套餐会更换菜品
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {//修改套餐  是不知道套餐id的 知道菜品id
        //这个地方我不是很理解为什么就是一定要传入setmeal来进行更新
        //这个地方是因为前端DTO中可能有多余字段，反正就是不建议传入DTO来，
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //获取套餐id
        Long id = setmealDTO.getId();
        //删除原来的套餐菜品关联数据
        setmealDishMapper.delete(id);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();//获取新的关系
        setmealDishes.forEach(setmealDish -> {//对于里面每一对关系  重新设置套餐id
            setmealDish.setSetmealId(id);
        });

        //重新插入新的关联数据
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}























