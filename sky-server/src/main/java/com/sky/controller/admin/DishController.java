package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;


@RestController
@Slf4j
@Api(tags = "菜品相关接口")
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品操作")
    public Result<Dish> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询操作")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);//这个地方必须返回pageResult这个，因为前端就是在Result中接受数据的
    }

    @DeleteMapping
    @ApiOperation("批量删除菜品功能")
    public Result<String> delete(@RequestParam List<Long> ids){
        //加注解以后  就可以将地址栏中多个数字参数提取出来然后变成List集合
        log.info("菜品批量删除：{}", ids);
        dishService.deleteBatch(ids);

        cleanCache("dish_*");

        return Result.success();
    }

    //根据id查询菜品
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜品")
    public Result<DishVO>getById(@PathVariable Long id){
        log.info("根据ID查询菜品：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    //修改菜品
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("需要修改的菜品是{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        cleanCache("dish_*");

        return Result.success();
    }

    //起售  停售菜品
    @PostMapping("/status/{status}")
    @ApiOperation("起售停售菜品")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("起售停售商品:{}", id);
        dishService.startOrStop(status, id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        cleanCache("dish_"+ dishVO.getCategoryId());

        return Result.success();
    }

    //根据分类id查询菜品
    //其实这个要是在菜品管理那边，也已经包含在分页查询那部分了
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        log.info("根据分类id查询菜品:{}", categoryId);
        List<Dish> list = dishService.listByCategoryId(categoryId);
        return Result.success(list);
    }


    /**
     * 清理缓存数据
     * @param pattern
     */
    private void cleanCache(String pattern){
        //因为这个地方可能会缓存很多个分类中的数据，所以我们就直接清理掉所有的菜品缓存数据
        //删除所有"dish_"开头的数据
        Set keys = redisTemplate.keys(pattern);//先查询出来所有的key  然后进行删除
        redisTemplate.delete(keys);//这里删除支持传进来一个keys集合
    }
}
