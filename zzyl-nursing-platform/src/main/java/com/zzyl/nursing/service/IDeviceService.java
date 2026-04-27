package com.zzyl.nursing.service;

import java.util.List;

import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.nursing.domain.Device;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.ProductVo;

/**
 * 设备Service接口
 * 
 * @author alexis
 * @date 2025-07-03
 */
public interface IDeviceService extends IService<Device>
{
    /**
     * 查询设备
     * 
     * @param id 设备主键
     * @return 设备
     */
    public Device selectDeviceById(Long id);

    /**
     * 查询设备列表
     * 
     * @param device 设备
     * @return 设备集合
     */
    public List<Device> selectDeviceList(Device device);

    /**
     * 新增设备
     * 
     * @param device 设备
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备
     * 
     * @param deviceDto 设备
     * @return 结果
     */
    public int updateDevice(DeviceDto deviceDto);

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的设备主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备信息
     * 
     * @param id 设备主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    /**
     * 从物联网平台同步产品列表
     */
    void syncProductList();

    /**
     * 从Redis中查询所有产品列表
     */
    List<ProductVo> allProduct();

    /**
     * 注册设备
     */
    void register(DeviceDto deviceDto);

    /**
     * 查询设备详情
     */
    DeviceDetailVo queryDeviceDetail(String iotid);

    /**
     * 查询设备上报的数据
     */
    AjaxResult queryServiceProperties(String iotId);

    /**
     * 删除设备
     */
    void deleteDevice(String iotId);

    /**
     * 查询产品详情
     */
    AjaxResult queryProduct(String productKey);
}
