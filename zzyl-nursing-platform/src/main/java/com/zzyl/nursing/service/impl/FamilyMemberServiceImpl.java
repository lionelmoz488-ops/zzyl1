package com.zzyl.nursing.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzyl.framework.web.service.TokenService;
import com.zzyl.nursing.dto.UserLoginRequestDto;
import com.zzyl.nursing.service.WechatService;
import com.zzyl.nursing.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.FamilyMemberMapper;
import com.zzyl.nursing.domain.FamilyMember;
import com.zzyl.nursing.service.IFamilyMemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 老人家属Service业务层处理
 * 
 * @author alexis
 * @date 2025-06-28
 */
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService
{
    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Autowired
    private WechatService wechatService;

    @Autowired
    private TokenService tokenService;

    static List<String> DEFAULT_NICKNAME_PREFIX = ListUtil.of(
            "生活更美好",
            "大桔大利",
            "日富一日",
            "好柿开花",
            "柿柿如意",
            "一椰暴富",
            "大柚所为",
            "杨梅吐气",
            "天生荔枝"
    );

    /**
     * 查询老人家属
     * 
     * @param id 老人家属主键
     * @return 老人家属
     */
    @Override
    public FamilyMember selectFamilyMemberById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询老人家属列表
     * 
     * @param familyMember 老人家属
     * @return 老人家属
     */
    @Override
    public List<FamilyMember> selectFamilyMemberList(FamilyMember familyMember)
    {
        return familyMemberMapper.selectFamilyMemberList(familyMember);
    }

    /**
     * 新增老人家属
     * 
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int insertFamilyMember(FamilyMember familyMember)
    {
        return save(familyMember) ? 1 : 0;
    }

    /**
     * 修改老人家属
     * 
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int updateFamilyMember(FamilyMember familyMember)
    {
        return updateById(familyMember) ? 1 : 0;
    }

    /**
     * 批量删除老人家属
     * 
     * @param ids 需要删除的老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除老人家属信息
     * 
     * @param id 老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 小程序端登录
     *
     * @param userLoginRequestDto dto对象
     * @return 登录结果
     */
    @Override
    public LoginVo login(UserLoginRequestDto userLoginRequestDto) {
        // 1.调用微信api，根据code获取openid
        String openId = wechatService.getOpenId(userLoginRequestDto.getCode());

        // 2.根据openid查询数据库，判断用户是否存在
        FamilyMember familyMember = getOne(Wrappers.<FamilyMember>lambdaQuery().eq(FamilyMember::getOpenId, openId));

        // 3.判断如果用户为空，创建新对象并设置openid
        if (ObjectUtil.isEmpty(familyMember)) {
            familyMember = FamilyMember.builder().openId(openId).build();
        }

        // 4.调用微信api获取用户的手机号
        String phone = wechatService.getPhone(userLoginRequestDto.getPhoneCode());
        familyMember.setPhone(phone);

        // 5.新增或更新用户信息
        saveOrUpdateFamilyMember(familyMember);

        // 6.把用户id和昵称封装到jwt返回
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", familyMember.getId());
        claims.put("nickName", familyMember.getName());

        String token = tokenService.createToken(claims);

        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        loginVo.setNickName(familyMember.getName());
        return loginVo;
    }

    /**
     * 新增或更新家属信息
     *
     * @param familyMember 家属信息
     */
    private void saveOrUpdateFamilyMember(FamilyMember familyMember) {
        if (ObjectUtil.isEmpty(familyMember.getId())) {
            // 为用户设置一个昵称
            String prefix = DEFAULT_NICKNAME_PREFIX.get((int) (Math.random() * DEFAULT_NICKNAME_PREFIX.size()));
            String nickName = prefix + familyMember.getPhone().substring(7);
            familyMember.setName(nickName);
            // 新增家属信息
            save(familyMember);
        } else {
            // 更新家属信息
            updateById(familyMember);
        }
    }
}
