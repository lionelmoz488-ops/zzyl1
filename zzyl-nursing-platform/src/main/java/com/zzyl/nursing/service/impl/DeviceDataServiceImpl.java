package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.constant.HttpStatus;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.utils.DateTimeZoneConverter;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.dto.DeviceDataPageReqDto;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.task.vo.IotMsgNotifyData;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.DeviceDataMapper;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.service.IDeviceDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 设备数据Service业务层处理
 * 
 * @author alexis
 * @date 2025-07-07
 */
@Service
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements IDeviceDataService
{
    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询设备数据
     * 
     * @param id 设备数据主键
     * @return 设备数据
     */
    @Override
    public DeviceData selectDeviceDataById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询设备数据列表
     *
     * @param dto 设备数据
     * @return 设备数据
     */
    @Override
    public TableDataInfo selectDeviceDataList(DeviceDataPageReqDto dto)
    {
        Page<DeviceData> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<DeviceData> queryWrapper = Wrappers.<DeviceData>lambdaQuery()
                .like(StringUtils.isNotEmpty(dto.getDeviceName()), DeviceData::getDeviceName, dto.getDeviceName())
                .eq(StringUtils.isNotEmpty(dto.getFunctionId()), DeviceData::getFunctionId, dto.getFunctionId())
                .between(ObjectUtils.isNotEmpty(dto.getStartTime()) && ObjectUtils.isNotEmpty(dto.getEndTime()),
                        DeviceData::getAlarmTime, dto.getStartTime(), dto.getEndTime());
        page = page(page, queryWrapper);
        return getTableDataInfo(page);
    }

    /**
     * 获取分页数据
     *
     */
    private TableDataInfo getTableDataInfo(Page<DeviceData> page) {
        TableDataInfo tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setMsg("请求成功");
        tableDataInfo.setRows(page.getRecords());
        tableDataInfo.setTotal(page.getTotal());
        return tableDataInfo;
    }

    /**
     * 新增设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int insertDeviceData(DeviceData deviceData)
    {
        return save(deviceData) ? 1 : 0;
    }

    /**
     * 修改设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int updateDeviceData(DeviceData deviceData)
    {
        return updateById(deviceData) ? 1 : 0;
    }

    /**
     * 批量删除设备数据
     * 
     * @param ids 需要删除的设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备数据信息
     * 
     * @param id 设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 批量插入设备数据
     *
     * @param iotMsgNotifyData 设备数据
     */
    @Override
    public void batchInsertDeviceData(IotMsgNotifyData iotMsgNotifyData) {
        // 解析上报数据中的设备id字段
        String iotId = iotMsgNotifyData.getHeader().getDeviceId();
        // 通过设备id到设备表中查询设备
        Device device = deviceMapper.selectOne(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotId));
        if (ObjectUtil.isEmpty(device)) {
//            log.error("设备不存在");
            return;
        }
        // 批量保存设备数据
        iotMsgNotifyData.getBody().getServices().forEach(s -> {
            // 获取properties中的物模型集合
            Map<String, Object> properties = s.getProperties();
            if (ObjectUtil.isEmpty(properties)) {
                return;
            }
            // 获取设备上报数据的时间
            String eventTimeStr = s.getEventTime();
            LocalDateTime localDateTime = LocalDateTimeUtil.parse(eventTimeStr, "yyyyMMdd'T'HHmmss'Z'");
            LocalDateTime eventTime = DateTimeZoneConverter.utcToShanghai(localDateTime);

            List<DeviceData> deviceDataList = new ArrayList<>();

            // 遍历properties，构建设备数据对象，并添加到一个集合中
            properties.forEach((key, value) -> {
                DeviceData deviceData = BeanUtil.toBean(device, DeviceData.class);
                deviceData.setId(null);
                deviceData.setCreateTime(null);
                deviceData.setFunctionId(key);
                deviceData.setDataValue(String.valueOf(value));
                deviceData.setAlarmTime(eventTime);
                deviceDataList.add(deviceData);
            });

            // 批量保存设备数据
            saveBatch(deviceDataList);

            // 将设备最近一次上报的数据保存到Redis中
            redisTemplate.opsForHash().put(CacheConstants.IOT_DEVICE_LAST_DATA, iotId, JSONUtil.toJsonStr(deviceDataList));
        });
    }
}
