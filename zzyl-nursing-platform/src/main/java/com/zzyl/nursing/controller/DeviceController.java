package com.zzyl.nursing.controller;

import java.util.List;

import com.huaweicloud.sdk.iotda.v5.model.ServiceProperty;
import com.zzyl.common.core.domain.R;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.ProductVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.service.IDeviceService;
import com.zzyl.common.core.page.TableDataInfo;

/**
 * 设备Controller
 * 
 * @author alexis
 * @date 2025-07-03
 */
@Api("设备管理")
@RestController
@RequestMapping("/nursing/device")
public class DeviceController extends BaseController
{
    @Autowired
    private IDeviceService deviceService;

    /**
     * 查询设备列表
     */
    @ApiOperation("查询设备列表")
    @PreAuthorize("@ss.hasPermi('nursing:device:list')")
    @GetMapping("/list")
    public TableDataInfo<List<Device>> list(@ApiParam("查询条件对象") Device device)
    {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    @ApiOperation("从物联网平台同步产品列表")
    @PostMapping("/syncProductList")
    public AjaxResult syncProductList() {
        deviceService.syncProductList();
        return success();
    }

    @ApiOperation("查询所有产品列表")
    @GetMapping("/allProduct")
    public R<List<ProductVo>> allProduct() {
        List<ProductVo> list = deviceService.allProduct();
        return R.ok(list);
    }

    @ApiOperation("设备注册")
    @PostMapping("/register")
    public AjaxResult register(@RequestBody DeviceDto deviceDto) {
        deviceService.register(deviceDto);
        return success();
    }

    @ApiOperation("获取设备详细信息")
    @GetMapping("/{iotid}")
    public R<DeviceDetailVo> queryDeviceDetail(@PathVariable String iotid) {
        DeviceDetailVo deviceDetailVo = deviceService.queryDeviceDetail(iotid);
        return R.ok(deviceDetailVo);
    }

    @ApiOperation("查看设备上报的数据")
    @GetMapping("/queryServiceProperties/{iotId}")
    public AjaxResult queryServiceProperties(@PathVariable String iotId) {
        return deviceService.queryServiceProperties(iotId);
    }

    @ApiOperation("修改设备")
    @PutMapping
    public AjaxResult updateDevice(@RequestBody DeviceDto deviceDto) {
        deviceService.updateDevice(deviceDto);
        return success();
    }

    @ApiOperation("删除设备")
    @DeleteMapping("/{iotId}")
    public AjaxResult deleteDevice(@PathVariable String iotId) {
        deviceService.deleteDevice(iotId);
        return success();
    }

    @ApiOperation("查询产品信息")
    @GetMapping("/queryProduct/{productKey}")
    public AjaxResult queryProduct(@PathVariable String productKey) {
        return deviceService.queryProduct(productKey);
    }
}
