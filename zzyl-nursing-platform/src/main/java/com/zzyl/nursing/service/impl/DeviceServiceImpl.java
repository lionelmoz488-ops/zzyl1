package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.*;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.DateTimeZoneConverter;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.ProductVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 设备Service业务层处理
 * 
 * @author alexis
 * @date 2025-07-03
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService
{
    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private IoTDAClient iotDAClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询设备
     * 
     * @param id 设备主键
     * @return 设备
     */
    @Override
    public Device selectDeviceById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询设备列表
     * 
     * @param device 设备
     * @return 设备
     */
    @Override
    public List<Device> selectDeviceList(Device device)
    {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 新增设备
     * 
     * @param device 设备
     * @return 结果
     */
    @Override
    public int insertDevice(Device device)
    {
        return save(device) ? 1 : 0;
    }

    /**
     * 修改设备
     * 
     * @param deviceDto 设备
     * @return 结果
     */
    @Override
    public int updateDevice(DeviceDto deviceDto)
    {
        // 修改IoT平台上的设备
        UpdateDeviceRequest request = new UpdateDeviceRequest();
        request.setDeviceId(deviceDto.getIotId());
        UpdateDevice body = new UpdateDevice();
        body.setDeviceName(deviceDto.getDeviceName());
        request.setBody(body);
        try {
            iotDAClient.updateDevice(request);
        } catch (Exception e) {
            throw new BaseException("调用IoT平台修改设备失败");
        }
        // 修改本地存储的设备
        Device device = BeanUtil.toBean(deviceDto, Device.class);
        boolean flag;
        try {
            flag = updateById(device);
        } catch (Exception e) {
            throw new BaseException("该老人/位置已绑定该类型的设备，请重新选择绑定位置");
        }
        return flag ? 1 : 0;
    }

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的设备主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备信息
     * 
     * @param id 设备主键
     * @return 结果
     */
    @Override
    public int deleteDeviceById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 从物联网平台同步产品列表
     */
    @Override
    public void syncProductList() {
        ListProductsRequest request = new ListProductsRequest();
        request.setLimit(50);
        ListProductsResponse response = iotDAClient.listProducts(request);
        // 判断响应是否成功
        if (response.getHttpStatusCode() != 200) {
            throw new BaseException("从IOT平台同步产品列表失败");
        }
        // 存储到redis
        redisTemplate.opsForValue().set(CacheConstants.IOT_ALL_PRODUCT_LIST, JSONUtil.toJsonStr(response.getProducts()));
    }

    /**
     * 从Redis中查询所有产品列表
     */
    @Override
    public List<ProductVo> allProduct() {
        // 从redis中查询数据
        String allProductList = redisTemplate.opsForValue().get(CacheConstants.IOT_ALL_PRODUCT_LIST);
        // 判断返回的数据是否为空
        if (StringUtils.isEmpty(allProductList)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(allProductList, ProductVo.class);
    }

    /**
     * 注册设备
     *
     * @param deviceDto
     */
    @Override
    public void register(DeviceDto deviceDto) {
        // 判断设备名称是否重复
        long count = count(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceName, deviceDto.getDeviceName()));
        if (count > 0) {
            throw new BaseException("设备名称重复，请重新输入");
        }
        // 判断设备标识是否重复
        count = count(Wrappers.<Device>lambdaQuery().eq(Device::getNodeId, deviceDto.getNodeId()));
        if (count > 0) {
            throw new BaseException("设备标识码重复，请重新输入");
        }

        // 如果是随身设备，将物理位置类型设置为-1
        if (deviceDto.getLocationType() == 0) {
            deviceDto.setPhysicalLocationType(-1);
        }

        // 判断同一个位置是否绑定了相同的产品
        count = count(Wrappers.<Device>lambdaQuery().eq(Device::getBindingLocation, deviceDto.getBindingLocation())
                .eq(Device::getLocationType, deviceDto.getLocationType())
                .eq(Device::getPhysicalLocationType, deviceDto.getPhysicalLocationType())
                .eq(Device::getProductKey, deviceDto.getProductKey()));
        if (count > 0) {
            throw new BaseException("该老人/位置已经绑定了该产品，请重新选择");
        }

        // 在华为云IoT平台上注册设备
        AddDeviceRequest request = new AddDeviceRequest();
        AddDevice body = new AddDevice();
        body.setProductId(deviceDto.getProductKey());
        body.setNodeId(deviceDto.getNodeId());
        body.setDeviceName(deviceDto.getDeviceName());
        // 设置设备密钥
        AuthInfo authInfo = new AuthInfo();
        String secret = UUID.randomUUID().toString().replace("-", "");
        authInfo.setSecret(secret);
        body.setAuthInfo(authInfo);
        request.setBody(body);
        AddDeviceResponse response;
        try {
            response = iotDAClient.addDevice(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("设备注册失败");
        }

        // 本地保存设备信息
        // 属性拷贝
        Device device = BeanUtil.toBean(deviceDto, Device.class);
        String deviceId = response.getDeviceId();
        // 设置设备ID
        device.setIotId(deviceId);
        // 设置设备密钥
        device.setSecret(secret);

        save(device);
    }

    /**
     * 查询设备详情
     *
     * @param iotid
     */
    @Override
    public DeviceDetailVo queryDeviceDetail(String iotid) {
        // 查询本地设备数据
        Device device = getOne(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotid));
        if (ObjectUtil.isEmpty(device)) {
            return null;
        }
        // 调用华为云接口查询设备详情
        ShowDeviceRequest request = new ShowDeviceRequest();
        request.setDeviceId(device.getIotId());
        ShowDeviceResponse response;
        try {
            response = iotDAClient.showDevice(request);
        } catch (Exception e) {
            throw new BaseException("查询设备详情失败");
        }
        // 合并两部分数据并返回
        // 属性拷贝
        DeviceDetailVo deviceDetailVo = BeanUtil.toBean(device, DeviceDetailVo.class);
        deviceDetailVo.setDeviceStatus(response.getStatus());
        String activeTimeStr = response.getActiveTime();
        if (StringUtils.isNotEmpty(activeTimeStr)) {
            LocalDateTime activeTime = LocalDateTimeUtil.parse(activeTimeStr, DatePattern.UTC_MS_PATTERN);
            deviceDetailVo.setActiveTime(DateTimeZoneConverter.utcToShanghai(activeTime));
        }

        return deviceDetailVo;
    }

    /**
     * 查询设备上报的数据
     *
     * @param iotId 设备ID
     */
    @Override
    public AjaxResult queryServiceProperties(String iotId) {
        ShowDeviceShadowRequest request = new ShowDeviceShadowRequest();
        request.setDeviceId(iotId);
        ShowDeviceShadowResponse response;
        try {
            response = iotDAClient.showDeviceShadow(request);
        } catch (Exception e) {
            throw new BaseException("查询设备属性失败");
        }
        // 从response中解析数据
        List<DeviceShadowData> shadow = response.getShadow();
        if (CollUtil.isEmpty(shadow)) {
            return AjaxResult.success(Collections.emptyList());
        }
        // 解析结果中的reported
        DeviceShadowProperties reported = shadow.get(0).getReported();
        // 解析properties
        JSONObject jsonObject = JSONUtil.parseObj(reported.getProperties());
        // 解析event_time
        String eventTimeStr = reported.getEventTime();
        // 将字符串日期解析为LocalDateTime
        LocalDateTime parsed = LocalDateTimeUtil.parse(eventTimeStr, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        LocalDateTime eventTime = DateTimeZoneConverter.utcToShanghai(parsed);

        // 封装结果数据并返回
        List<Map<String, Object>> result = new ArrayList<>();
        jsonObject.forEach((key, value) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("eventTime", eventTime);
            map.put("functionId", key);
            map.put("value", value);
            result.add(map);
        });
        return AjaxResult.success(result);
    }

    /**
     * 删除设备
     *
     * @param iotId 设备ID
     */
    @Override
    public void deleteDevice(String iotId) {
        // 删除IoTDA平台上的设备
        DeleteDeviceRequest request = new DeleteDeviceRequest();
        request.setDeviceId(iotId);
        try {
            iotDAClient.deleteDevice(request);
        } catch (Exception e) {
            throw new BaseException("调用IoT平台设备删除失败");
        }
        // 删除本地存储的设备
        remove(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotId));
    }

    /**
     * 查询产品详情
     *
     * @param productKey    产品key
     */
    @Override
    public AjaxResult queryProduct(String productKey) {
        // 参数校验
        if (StringUtils.isEmpty(productKey)) {
            throw new BaseException("产品key不能为空");
        }
        // 调用华为云接口查询产品详情
        ShowProductRequest showProductRequest = new ShowProductRequest();
        showProductRequest.setProductId(productKey);
        ShowProductResponse response;
        try {
            response = iotDAClient.showProduct(showProductRequest);
        } catch (Exception e) {
            throw new BaseException("查询产品详情失败");
        }
        // 解析结果并判断结果中是否有咱们需要的数据
        List<ServiceCapability> serviceCapabilities = response.getServiceCapabilities();
        if (CollUtil.isEmpty(serviceCapabilities)) {
            return AjaxResult.success(Collections.emptyList());
        }
        return AjaxResult.success(serviceCapabilities);
    }
}
