package com.zzyl.nursing.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.zzyl.common.core.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.AlertData;
import com.zzyl.nursing.service.IAlertDataService;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.common.core.page.TableDataInfo;

/**
 * 报警数据Controller
 * 
 * @author alexis
 * @date 2025-07-16
 */
@Api("报警数据管理")
@RestController
@RequestMapping("/nursing/alertData")
public class AlertDataController extends BaseController
{
    @Autowired
    private IAlertDataService alertDataService;

    /**
     * 查询报警数据列表
     */
    @ApiOperation("查询报警数据列表")
    @PreAuthorize("@ss.hasPermi('nursing:alertData:list')")
    @GetMapping("/list")
    public TableDataInfo<List<AlertData>> list(@ApiParam("查询条件对象") AlertData alertData)
    {
        startPage();
        List<AlertData> list = alertDataService.selectAlertDataList(alertData);
        return getDataTable(list);
    }

    /**
     * 导出报警数据列表
     */
    @ApiOperation("导出报警数据列表")
    @PreAuthorize("@ss.hasPermi('nursing:alertData:export')")
    @Log(title = "报警数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@ApiParam("导出的查询条件") HttpServletResponse response, AlertData alertData)
    {
        List<AlertData> list = alertDataService.selectAlertDataList(alertData);
        ExcelUtil<AlertData> util = new ExcelUtil<AlertData>(AlertData.class);
        util.exportExcel(response, list, "报警数据数据");
    }

    /**
     * 获取报警数据详细信息
     */
    @ApiOperation("获取报警数据详细信息")
    @PreAuthorize("@ss.hasPermi('nursing:alertData:query')")
    @GetMapping(value = "/{id}")
    public R<AlertData> getInfo(@PathVariable("id") @ApiParam("报警数据ID") Long id)
    {
        return R.ok(alertDataService.selectAlertDataById(id));
    }

    /**
     * 新增报警数据
     */
    @ApiOperation("新增报警数据")
    @PreAuthorize("@ss.hasPermi('nursing:alertData:add')")
    @Log(title = "报警数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody @ApiParam("新增的报警数据对象") AlertData alertData)
    {
        return toAjax(alertDataService.insertAlertData(alertData));
    }

    /**
     * 修改报警数据
     */
    @ApiOperation("修改报警数据")
    @PreAuthorize("@ss.hasPermi('nursing:alertData:edit')")
    @Log(title = "报警数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody @ApiParam("修改的报警数据对象") AlertData alertData)
    {
        return toAjax(alertDataService.updateAlertData(alertData));
    }

    /**
     * 删除报警数据
     */
    @ApiOperation("删除报警数据")
    @PreAuthorize("@ss.hasPermi('nursing:alertData:remove')")
    @Log(title = "报警数据", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable @ApiParam("要删除的报警数据ID") Long[] ids)
    {
        return toAjax(alertDataService.deleteAlertDataByIds(ids));
    }
}
