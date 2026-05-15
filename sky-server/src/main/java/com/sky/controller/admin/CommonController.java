package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.QiniuUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 这里面是通用接口
 */


@RestController
@Slf4j
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    @Autowired
    private QiniuUtils qiniuUtils;

    @PostMapping("/upload")
    @ApiOperation("文件上传")

    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);


        //调用七牛云OSS工具类
        try {
            //获取文件名称
            String originalFilename = file.getOriginalFilename();

            //获取后缀名
            String subffix = originalFilename.substring(originalFilename.lastIndexOf("."));

            //为了避免同名覆盖问题，构建新的文件名
            String fileName = UUID.randomUUID().toString() + subffix;
            String url = qiniuUtils.uploadByBytes(file.getBytes(), fileName);
            return Result.success(url);

        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }

    }
}
