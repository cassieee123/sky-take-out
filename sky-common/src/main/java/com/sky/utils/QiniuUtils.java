package com.sky.utils;


import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 一个封装好的工具类，专门把图片/文件上传到七牛云的工具
 */

@Component
public class QiniuUtils {
    // 通过 @Value 注解从配置文件中读取值
    @Value("${sky.qiniu.accessKey}")
    private String accessKey;

    @Value("${sky.qiniu.secretKey}")
    private String secretKey;

    @Value("${sky.qiniu.bucket}")
    private String bucket;

    @Value("${sky.qiniu.hostName}")
    private String hostName;

    public String uploadByBytes(byte[] bytes,String fileName){

        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;// 指定分片上传版本
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);

        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = fileName;

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);

        try {
            Response response = uploadManager.put(bytes, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            System.out.println(putRet.key);
            System.out.println(putRet.hash);
            return hostName+"/" +putRet.key;
        } catch (QiniuException ex) {
            ex.printStackTrace();
            if (ex.response != null) {
                System.err.println(ex.response);
                try {
                    String body = ex.response.toString();
                    System.err.println(body);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     *删除图片
     * @param key
     * @return
     */
    public String delete(String key){
        Auth auth = Auth.create(accessKey,secretKey);
        Configuration cfg = new Configuration(Region.autoRegion());
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            bucketManager.delete(bucket,key);
        }catch (QiniuException Q){
            return Q.toString();
        }
        System.out.println("success");
        return null;
    }

}