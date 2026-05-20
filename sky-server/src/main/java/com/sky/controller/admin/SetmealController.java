package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api(tags = "套餐相关接口")
@RequestMapping("/admin/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
            log.info("新增套餐:{}", setmealDTO);
            setmealService.saveWithDish(setmealDTO);
            return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("下面执行分页查询:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除套餐
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除套餐：{}", ids);
        setmealService.deletBatch(ids);
        return Result.success();
    }

    /**
     * 根据ID查询套餐
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据ID查询套餐 {}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    /**
     * 套餐起售  停售
     */
    @PostMapping("/status/{status}")
    @ApiOperation("套餐起售  停售")
    public Result startOrStop(@PathVariable Integer status, @RequestParam Long id){
        log.info("对当前套餐起售或停售, {}, {}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 修改套餐
     */
    @PutMapping
    @ApiOperation("修改套餐")
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改当前套餐:{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }
}
