package com.zzyl.framework.interceptor;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.common.utils.UserThreadLocal;
import com.zzyl.framework.web.service.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MemberInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是Controller层的请求，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 获取header中的token
        String token = request.getHeader("authorization");

        // 判断token是否为空，如果为空，响应401，重新登录
        if (StringUtils.isEmpty(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BaseException("认证失败");
        }

        // 解析token，如果解析失败，响应401，重新登录
        Claims claims;
        try {
            claims = tokenService.parseToken(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BaseException("认证失败");
        }

        if (ObjectUtil.isEmpty(claims)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BaseException("认证失败");
        }

        // 获取token中的userId数据
        Long userId = MapUtil.getLong(claims, "userId");

        // 如果获取的数据为空，响应401，重新登录
        if (ObjectUtil.isEmpty(userId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BaseException("认证失败");
        }

        // 将解析到的userId数据保存到ThreadLocal中
        UserThreadLocal.set(userId);

        // 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserThreadLocal.remove();
    }
}
