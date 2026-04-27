package com.zzyl.nursing.controller.member;

import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.core.domain.R;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.utils.UserThreadLocal;
import com.zzyl.nursing.domain.Reservation;
import com.zzyl.nursing.dto.ReservationDto;
import com.zzyl.nursing.service.IReservationService;
import com.zzyl.nursing.vo.TimeCountVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约信息Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/member/reservation")
@Api(tags =  "预约信息相关接口")
public class MemberReservationController extends BaseController
{
    @Autowired
    private IReservationService reservationService;

    @GetMapping("/cancelled-count")
    @ApiOperation("查询取消预约数量")
    public R<Integer> getCancelledReservationCount() {
        Long userId = UserThreadLocal.getUserId();
        int count = reservationService.getCancelledCount(userId);
        return R.ok(count);
    }

    @GetMapping("/countByTime")
    @ApiOperation("查询每个时间段剩余预约次数")
    public R<List<TimeCountVo>> countReservationsForEachTimeWithinTimeRange(Long time) {
        List<TimeCountVo> list = reservationService.countReservationsForTime(time);
        return R.ok(list);
    }

    /**
     * 新增预约信息
     */
    @PostMapping
    @ApiOperation("新增预约信息")
    public AjaxResult add(@RequestBody ReservationDto reservationDto) {
        return toAjax(reservationService.insertReservation(reservationDto));
    }

    /*
     *分页查询增加预约人姓名，手机号，状态，类型的查询条件
     */
    @GetMapping("/page")
    @ApiOperation("分页查询预约")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "status", value = "状态", dataType = "int", paramType = "query"),
    })
    public R<TableDataInfo<Reservation>> findByPage(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) Integer status) {
        TableDataInfo<Reservation> tableDataInfo = reservationService.selectByPage(pageNum, pageSize, status);
        return R.ok(tableDataInfo);
    }

    /**
     * 取消预约
     */
    @PutMapping("/{id}/cancel")
    @ApiOperation("取消预约")
    public AjaxResult cancel(@PathVariable Long id) {
        return toAjax(reservationService.cancelReservation(id));
    }
}