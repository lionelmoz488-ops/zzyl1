package com.zzyl.nursing.service.impl;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.vo.NursingProjectVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.NursingProjectMapper;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.service.INursingProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 护理项目Service业务层处理
 * 
 * @author alexis
 * @date 2025-06-02
 */
@Service
public class NursingProjectServiceImpl extends ServiceImpl<NursingProjectMapper, NursingProject> implements INursingProjectService
{
    @Autowired
    private NursingProjectMapper nursingProjectMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 查询护理项目
     * 
     * @param id 护理项目主键
     * @return 护理项目
     */
    @Override
    public NursingProject selectNursingProjectById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询护理项目列表
     * 
     * @param nursingProject 护理项目
     * @return 护理项目
     */
    @Override
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject)
    {
        return nursingProjectMapper.selectNursingProjectList(nursingProject);
    }

    /**
     * 新增护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int insertNursingProject(NursingProject nursingProject)
    {
        boolean flag = save(nursingProject);
        // 删除缓存
        deleteCache();
        return flag ? 1 : 0;
    }

    /**
     * 删除缓存
     */
    private void deleteCache() {
        redisTemplate.delete(CacheConstants.NURSING_PROJECT_ALL_KEY);
    }

    /**
     * 修改护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int updateNursingProject(NursingProject nursingProject)
    {
        boolean flag = updateById(nursingProject);
        // 删除缓存
        deleteCache();
        return flag ? 1 : 0;
    }

    /**
     * 批量删除护理项目
     * 
     * @param ids 需要删除的护理项目主键
     * @return 结果
     */
    @Override
    public int deleteNursingProjectByIds(Long[] ids)
    {
        boolean flag = removeByIds(Arrays.asList(ids));
        // 删除缓存
        deleteCache();
        return flag ? 1 : 0;
    }

    /**
     * 删除护理项目信息
     * 
     * @param id 护理项目主键
     * @return 结果
     */
    @Override
    public int deleteNursingProjectById(Long id)
    {
        boolean flag = removeById(id);
        // 删除缓存
        deleteCache();
        return flag ? 1 : 0;
    }

    /**
     * 查询所有护理项目
     *
     * @return 护理项目列表
     */
    @Override
    public List<NursingProjectVo> getAll() {
        // 从缓存中查询所有护理项目
        List<NursingProjectVo> list = (List<NursingProjectVo>) redisTemplate.opsForValue().get(CacheConstants.NURSING_PROJECT_ALL_KEY);

        // 如果缓存中查到了，直接返回
        if (ObjectUtil.isNotEmpty(list)) {
            return list;
        }

        // 如果缓存中没有查到，则从数据库中查询，并将查询到的结果放入缓存中
        list = nursingProjectMapper.getAll();

        // 将查询到的结果放入缓存
        redisTemplate.opsForValue().set(CacheConstants.NURSING_PROJECT_ALL_KEY, list);
        return list;
    }

    /**
     * 分页查询护理项目（小程序端）
     * @param pageNum 当前页码
     * @param pageSize  每页显示记录数
     * @param name  护理项目名称
     * @param status    状态
     * @return  分页数据
     */
    @Override
    public TableDataInfo<NursingProject> pageByNameAndStatus(Integer pageNum, Integer pageSize, String name, Integer status) {
        Page<NursingProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<NursingProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, NursingProject::getName, name)
                .eq(status != null, NursingProject::getStatus, status);
        page = page(page, queryWrapper);

        TableDataInfo<NursingProject> tableDataInfo = buildTableData(page);
        return tableDataInfo;
    }

    private static TableDataInfo<NursingProject> buildTableData(Page<NursingProject> page) {
        TableDataInfo<NursingProject> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setRows(page.getRecords());
        tableDataInfo.setTotal(page.getTotal());
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("查询成功");
        return tableDataInfo;
    }
}
