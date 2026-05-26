package com.sky.context;

/**
 * 就是用来：在项目任何地方，获取当前登录用户ID
 * 比如，在写购物车、订单、地址这些功能时，每张表都必须存userId（谁操作的）
 */
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
