package com.zzyl.nursing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.UserThreadLocal;
import com.zzyl.nursing.domain.Reservation;
import com.zzyl.nursing.dto.ReservationDto;
import com.zzyl.nursing.mapper.ReservationMapper;
import com.zzyl.nursing.service.IReservationService;
import com.zzyl.nursing.vo.TimeCountVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约信息Service业务层处理
 * 
 * @author alexis
 * @date 2024-11-11
 */
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements IReservationService
{
    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 查询取消预约次数
     *
     * @param userId    用户id
     * @return  取消预约次数
     */
    @Override
    public int getCancelledCount(Long userId) {
        // 2024/11/11 00:00:00
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        // 2024/10/12 00:00:00
        LocalDateTime endTime = startTime.plusDays(1);

        LambdaQueryWrapper<Reservation> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Reservation::getUpdateBy, userId)
                .eq(Reservation::getStatus, 2)
                .between(Reservation::getUpdateTime, startTime, endTime);

        return (int) count(lambdaQueryWrapper);
    }

    /**
     * 查询每个时间段剩余预约次数
     *
     * @return 结果
     */
    @Override
    public List<TimeCountVo> countReservationsForTime(Long time) {
        LocalDateTime localDateTime = LocalDateTimeUtil.of(time);

        LocalDateTime startTime = localDateTime.toLocalDate().atStartOfDay();

        LocalDateTime endTime = startTime.plusDays(1);
        List<TimeCountVo> timeCountVoList = reservationMapper.countReservationsForTime(startTime, endTime);
        return timeCountVoList;
    }

    /**
     * 分页查询预约信息
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public TableDataInfo<Reservation> selectByPage(int pageNum, int pageSize, Integer status) {
        Long userId = UserThreadLocal.getUserId();
        if (ObjectUtil.isEmpty(userId)) {
            throw new BaseException("请先登录");
        }
        LambdaQueryWrapper<Reservation> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Reservation::getCreateBy, userId);
        lambdaQueryWrapper.eq(ObjectUtil.isNotEmpty(status), Reservation::getStatus, status);
        lambdaQueryWrapper.orderByDesc(Reservation::getCreateTime);
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        page = page(page, lambdaQueryWrapper);

        return createTableDataInfo(page);
    }

    private TableDataInfo<Reservation> createTableDataInfo(Page<Reservation> page) {
        TableDataInfo<Reservation> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setRows(page.getRecords());
        tableDataInfo.setTotal(page.getTotal());
        tableDataInfo.setMsg("请求成功");
        tableDataInfo.setCode(200);
        return tableDataInfo;
    }

    /**
     * 取消预约
     *
     * @param id
     * @return
     */
    @Override
    public int cancelReservation(Long id) {
        Reservation reservation = getById(id);
        if (ObjectUtil.isEmpty(reservation)) {
            throw new BaseException("预约不存在");
        }
        reservation.setStatus(2);
        reservation.setUpdateBy(String.valueOf(UserThreadLocal.getUserId()));
        return updateById(reservation) ? 1 : 0;
    }

    /**
     * 新增预约信息
     *
     * @param reservationDto 预约信息
     * @return 结果
     */
    @Override
    public int insertReservation(ReservationDto reservationDto) {
        Long userId = UserThreadLocal.getUserId();
        Reservation reservation = BeanUtil.toBean(reservationDto, Reservation.class);
        reservation.setCreateBy(String.valueOf(userId));
        reservation.setUpdateBy(String.valueOf(userId));
        reservation.setStatus(0);
        try {
            return save(reservation) ? 1 : 0;
        } catch (Exception e) {
            throw new BaseException("不能重复预约哦");
        }
    }

    /**
     * 定时更新过期预约的状态
     */
    @Override
    public void updateReservationStatus() {
        // 查询预约时间小于当前时间减30分钟且状态为0-待报到的预约
        List<Reservation> reservations = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .lt(Reservation::getTime, LocalDateTime.now().minusMinutes(30))
                .eq(Reservation::getStatus, 0));

        // 设置状态为已过期
        reservations.forEach(reservation -> reservation.setStatus(3));
        // 批量更新
        updateBatchById(reservations);
    }

}
