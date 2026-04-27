package com.zzyl.nursing.service;

public interface WechatService {
    /**
     * 获取微信用户openId
     * @param code  小程序端临时登录凭证
     * @return  openid
     */
    String getOpenId(String code);

    /**
     * 获取微信用户手机号
     * @param detailCode  小程序端手机号临时登录凭证
     * @return  手机号
     */
    String getPhone(String detailCode);
}
