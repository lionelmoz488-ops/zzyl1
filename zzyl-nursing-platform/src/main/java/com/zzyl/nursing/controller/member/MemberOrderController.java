package com.zzyl.nursing.controller.member;

import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.R;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.service.INursingProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约信息Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/member/orders/project")
@Api(tags =  "预约信息相关接口")
public class MemberOrderController extends BaseController
{
    @Autowired
    private INursingProjectService nursingProjectService;

    @GetMapping("/page")
    @ApiOperation("分页查询护理项目列表")
    public TableDataInfo<NursingProject> page(Integer pageNum, Integer pageSize, String name, Integer status) {
        return nursingProjectService.pageByNameAndStatus(pageNum, pageSize, name, status);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取护理项目详细信息")
    public R<NursingProject> getInfo(@ApiParam(value = "护理项目ID", required = true)
                                    @PathVariable Long id) {
        return R.ok(nursingProjectService.selectNursingProjectById(id));
    }

}