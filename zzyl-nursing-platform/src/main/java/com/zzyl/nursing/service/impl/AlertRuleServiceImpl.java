package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.config.WebSocketServer;
import com.zzyl.nursing.domain.AlertData;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.service.IAlertDataService;
import com.zzyl.nursing.vo.AlertNotifyVo;
import com.zzyl.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.AlertRuleMapper;
import com.zzyl.nursing.domain.AlertRule;
import com.zzyl.nursing.service.IAlertRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 报警规则Service业务层处理
 * 
 * @author alexis
 * @date 2025-07-14
 */
@Service
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements IAlertRuleService
{
    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private IAlertDataService alertDataService;

    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${alert.deviceMaintainerRole}")
    private String deviceMaintainerRole;

    @Value("${alert.managerRole}")
    private String managerRole;

    /**
     * 查询报警规则
     * 
     * @param id 报警规则主键
     * @return 报警规则
     */
    @Override
    public AlertRule selectAlertRuleById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询报警规则列表
     * 
     * @param alertRule 报警规则
     * @return 报警规则
     */
    @Override
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule)
    {
        return alertRuleMapper.selectAlertRuleList(alertRule);
    }

    /**
     * 新增报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int insertAlertRule(AlertRule alertRule)
    {
        return save(alertRule) ? 1 : 0;
    }

    /**
     * 修改报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int updateAlertRule(AlertRule alertRule)
    {
        return updateById(alertRule) ? 1 : 0;
    }

    /**
     * 批量删除报警规则
     * 
     * @param ids 需要删除的报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除报警规则信息
     * 
     * @param id 报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 报警规则过滤
     */
    @Override
    public void alertFilter() {
        // 报警规则过滤
        // 查询所有报警规则并判断规则是否为空
        long count = count(Wrappers.<AlertRule>lambdaQuery().eq(AlertRule::getStatus, 1));
        if (count <= 0) {
            return;
        }
        // 获取所有设备最近一次上报的数据
        List<Object> values = redisTemplate.opsForHash().values(CacheConstants.IOT_DEVICE_LAST_DATA);
        if (CollUtil.isEmpty(values)) {
            return;
        }
        // 解析处理设备上报的数据
        List<DeviceData> deviceDataList = new ArrayList<>();
        values.forEach(value -> {
            deviceDataList.addAll(JSONUtil.toList(value.toString(), DeviceData.class));
        });
        // 遍历报警数据集合，逐条处理
        deviceDataList.forEach(deviceData -> alertFilter(deviceData));
    }

    /**
     * 逐条过滤报警数据
     *
     * @param deviceData 设备数据
     */
    private void alertFilter(DeviceData deviceData) {
        // 判断数据的上报时间是否超过了1分钟
        LocalDateTime alarmTime = deviceData.getAlarmTime();
        long between = LocalDateTimeUtil.between(alarmTime, LocalDateTime.now(), ChronoUnit.SECONDS);
        if (between > 60) {
            return;
        }
        // 获取设备数据对应的报警规则
        List<AlertRule> allRules = list(Wrappers.<AlertRule>lambdaQuery()
                .eq(AlertRule::getProductKey, deviceData.getProductKey())
                .eq(AlertRule::getIotId, "-1")
                .eq(AlertRule::getFunctionId, deviceData.getFunctionId())
                .eq(AlertRule::getStatus, 1));

        List<AlertRule> iotIdRules = list(Wrappers.<AlertRule>lambdaQuery()
                .eq(AlertRule::getProductKey, deviceData.getProductKey())
                .eq(AlertRule::getIotId, deviceData.getIotId())
                .eq(AlertRule::getFunctionId, deviceData.getFunctionId())
                .eq(AlertRule::getStatus, 1));

        // 合并两部分规则
        Collection<AlertRule> alertRules = CollUtil.addAll(allRules, iotIdRules);
        // 判断合并后的集合是否为空
        if (CollUtil.isEmpty(alertRules)) {
            return;
        }
        // 遍历规则集合，并和上报的数据进行多层校验
        alertRules.forEach(alertRule -> deviceDataAlarmHandler(alertRule, deviceData));
    }

    /**
     * 过滤数据是否能触发报警规则
     *
     * @param alertRule 报警规则
     * @param deviceData 设备数据
     */
    private void deviceDataAlarmHandler(AlertRule alertRule, DeviceData deviceData) {
        // 判断设备上报数据的时间是否在规则的有效时间范围内
        String[] split = alertRule.getAlertEffectivePeriod().split("~");
        LocalTime startTime = LocalTime.parse(split[0], DateTimeFormatter.ofPattern("HH:mm:ss"));
        LocalTime endTime = LocalTime.parse(split[1], DateTimeFormatter.ofPattern("HH:mm:ss"));
        // 获取数据上报的时间
        LocalTime time = deviceData.getAlarmTime().toLocalTime();
        // 判断数据上报时间是否在规则的有效范围内
        if (time.isBefore(startTime) || time.isAfter(endTime)) {
            return;
        }
        // 定义统计达到了阈值的次数的key
        String aggCountKey = CacheConstants.ALERT_TRIGGER_COUNT_PREFIX + deviceData.getIotId() + ":" + deviceData.getFunctionId() + ":" + alertRule.getId();
        // 判断设备上报的数据是否达到了阈值
        Double value = alertRule.getValue();
        // 获取设备上报的数据
        Double deviceValue = Double.valueOf(deviceData.getDataValue());
        // 如果设备上报的数据大于阈值，则返回1；如果设备上报的数据小于阈值，则返回-1；如果设备上报的数据等于阈值，则返回0
        int compare = NumberUtil.compare(deviceValue, value);
        if (">=".equals(alertRule.getOperator()) && compare >= 0 || "<".equals(alertRule.getOperator()) && compare < 0) {
            // 数据达到了阈值
            log.info("设备数据达到了阈值：{}", deviceData);
        } else {
            // 数据没有达到阈值
            // 删除Redis中的报警数
            redisTemplate.delete(aggCountKey);
            return;
        }
        // 异常数据会执行到这里
        // 判断是否在沉默周期内
        String silentKey = CacheConstants.ALERT_SILENT_PREFIX + deviceData.getIotId() + ":" + deviceData.getFunctionId() + ":" + alertRule.getId();
        String silentData = redisTemplate.opsForValue().get(silentKey);
        if (StringUtils.isNotEmpty(silentData)) {
            // 在沉默周期内
            return;
        }
        // 判断数据是否达到了持续周期
        String aggData = redisTemplate.opsForValue().get(aggCountKey);
        int count = StringUtils.isEmpty(aggData) ? 1 : Integer.parseInt(aggData) + 1;
        // 判断count是否等于持续周期
        if (ObjectUtil.notEqual(count, alertRule.getDuration())) {
            // 不等于持续周期
            redisTemplate.opsForValue().set(aggCountKey, String.valueOf(count));
            return;
        }
        // count等于持续周期了，将这条数据保存到报警数据表中
        // 删除Redis中的报警数
        redisTemplate.delete(aggCountKey);
        // 添加沉默周期
        redisTemplate.opsForValue().set(silentKey, "1", alertRule.getAlertSilentPeriod(), TimeUnit.MINUTES);
        // 将数据保存到数据库中
        // 查询要保存报警数据的通知人
        List<Long> userIds = new ArrayList<>();
        if (alertRule.getAlertDataType().equals(0)) {
            // 老人异常数据
            if (deviceData.getLocationType().equals(0)) {
                // 随身设备
                userIds = deviceMapper.selectNursingIdsByIotIdWithElder(deviceData.getIotId());
            } else {
                // 固定设备
                userIds = deviceMapper.selectNursingIdsByIotIdWithBed(deviceData.getIotId());
            }
        } else {
            // 设备异常数据
            userIds = sysUserRoleMapper.selectUserIdByRoleName(deviceMaintainerRole);
        }
        // 不论是哪种类型的数据，都需要通知超级管理员
        List<Long> managerIds = sysUserRoleMapper.selectUserIdByRoleName(managerRole);
        Collection<Long> allUserIds = CollUtil.addAll(userIds, managerIds);
        // 去重
        allUserIds = CollUtil.distinct(allUserIds);

        // 批量保存报警数据
        List<AlertData> alertDataList = insertAlertData(allUserIds, alertRule, deviceData);

        // webSocket发送报警通知
        webSocketNotify(allUserIds, alertRule, alertDataList.get(0));
    }

    /**
     * webSocket发送报警通知
     * @param allUserIds 报警通知人id集合
     * @param alertRule 报警规则
     * @param alertData 报警数据
     */
    private void webSocketNotify(Collection<Long> allUserIds, AlertRule alertRule, AlertData alertData) {
        // 属性拷贝
        AlertNotifyVo alertNotifyVo = BeanUtil.toBean(alertData, AlertNotifyVo.class);
        alertNotifyVo.setAccessLocation(alertData.getRemark());
        alertNotifyVo.setFunctionName(alertRule.getFunctionName());
        alertNotifyVo.setAlertDataType(alertRule.getAlertDataType());
        alertNotifyVo.setNotifyType(1);
        webSocketServer.sendMessageToConsumer(alertNotifyVo, allUserIds);
    }

    /**
     * 批量保存报警数据
     * @param allUserIds 报警通知人id集合
     * @param alertRule 报警规则
     * @param deviceData 设备数据
     */
    private List<AlertData> insertAlertData(Collection<Long> allUserIds, AlertRule alertRule, DeviceData deviceData) {
        // 属性拷贝
        AlertData alertData = BeanUtil.toBean(deviceData, AlertData.class);
        alertData.setAlertRuleId(alertRule.getId());
        // 报警原因：心率<=60，持续3个周期预警
        String alertReason = CharSequenceUtil.format("{}{}{}，持续{}个周期预警", alertRule.getFunctionName(), alertRule.getOperator(), alertRule.getValue(), alertRule.getDuration());
        alertData.setAlertReason(alertReason);
        alertData.setType(alertRule.getAlertDataType());
        alertData.setStatus(0);
        // 遍历allUserIds通知人
        List<AlertData> list = allUserIds.stream().map(userId -> {
            // 属性拷贝
            AlertData dbAlertData = BeanUtil.toBean(alertData, AlertData.class);
            dbAlertData.setUserId(userId);
            dbAlertData.setId(null);
            dbAlertData.setCreateTime(null);
            return dbAlertData;
        }).collect(Collectors.toList());
        // 批量保存报警数据
        alertDataService.saveBatch(list);
        return list;
    }
}
