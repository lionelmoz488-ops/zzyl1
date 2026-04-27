package com.zzyl.framework.interceptor;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.zzyl.common.core.domain.model.LoginUser;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Autowired
    private HttpServletRequest request;

    public boolean isExclude() {
        try {
            String requestURI = request.getRequestURI();
            if (requestURI.startsWith("/member")) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        if (!isExclude()) {
            this.strictInsertFill(metaObject, "createBy", String.class, String.valueOf(getLoginUserId()));
        }
        this.strictInsertFill(metaObject, "createTime", Date.class, DateUtils.getNowDate());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (!isExclude()) {
            this.setFieldValByName("updateBy", String.valueOf(getLoginUserId()), metaObject);
        }
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }

    public Long getLoginUserId() {
        try {
            // 获取到当前登录人的信息
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (ObjectUtils.isNotEmpty(loginUser)) {
                return loginUser.getUserId();
            }
            return 1L;
        } catch (Exception e) {
            return 1L;
        }
    }

}
