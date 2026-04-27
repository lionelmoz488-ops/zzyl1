package com.zzyl.nursing.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.domain.Room;
import com.zzyl.nursing.mapper.RoomMapper;
import com.zzyl.nursing.service.IRoomService;
import com.zzyl.nursing.vo.DeviceInfo;
import com.zzyl.nursing.vo.RoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 房间Service业务层处理
 *
 * @author ruoyi
 * @date 2024-04-26
 */
@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements IRoomService {
    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询房间
     *
     * @param id 房间主键
     * @return 房间
     */
    @Override
    public Room selectRoomById(Long id) {
        return getById(id);
    }

    /**
     * 查询房间列表
     *
     * @param room 房间
     * @return 房间
     */
    @Override
    public List<Room> selectRoomList(Room room) {
        return roomMapper.selectRoomList(room);
    }

    /**
     * 新增房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int insertRoom(Room room) {
        return save(room) ? 1 : 0;
    }

    /**
     * 修改房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int updateRoom(Room room) {
        return updateById(room) ? 1 : 0;
    }

    /**
     * 批量删除房间
     *
     * @param ids 需要删除的房间主键
     * @return 结果
     */
    @Override
    public int deleteRoomByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 根据楼层 id 获取房间视图对象列表
     *
     * @param floorId
     * @return
     */
    @Override
    public List<RoomVo> getRoomsByFloorId(Long floorId) {
        return roomMapper.selectByFloorId(floorId);
    }


    /**
     * 获取所有房间（负责老人）
     *
     * @param floorId
     * @return
     */
    @Override
    public List<RoomVo> getRoomsWithNurByFloorId(Long floorId) {
        return roomMapper.selectByFloorIdWithNur(floorId);
    }

    /**
     * 根据房间 id 据房间id查询房间数据(楼层、房间、价格)
     *
     * @param id 房间id
     * @return 结果
     */
    @Override
    public RoomVo getRoomById(Long id) {
        return roomMapper.getRoomById(id);
    }

    /**
     * 根据楼层id获取房间中的智能设备及数据
     *
     * @param floorId 楼层id
     * @return 结果
     */
    @Override
    public List<RoomVo> getRoomsWithDeviceByFloorId(Long floorId) {
        List<RoomVo> roomVos = roomMapper.getRoomsWithDeviceByFloorId(floorId);
        roomVos.forEach(roomVo -> {
            // 获取到房间中的设备集合并遍历
            List<DeviceInfo> deviceVos = roomVo.getDeviceVos();
            deviceVos.forEach(deviceVo -> {
                // 获取到设备的iotid
                String iotId = deviceVo.getIotId();
                // 从Redis中查询这个设备最近一次上报的数据
                String jsonStr = (String) redisTemplate.opsForHash().get(CacheConstants.IOT_DEVICE_LAST_DATA, iotId);
                if (StringUtils.isEmpty(jsonStr)) {
                    return;     // 跳出本次循环，并不是结束方法
                }
                deviceVo.setDeviceDataVos(JSONUtil.toList(jsonStr, DeviceData.class));
            });
            // 获取到房间中的床位集合并遍历
            roomVo.getBedVoList().forEach(bedVo -> {
                // 获取到床位中的设备集合并遍历
                bedVo.getDeviceVos().forEach(deviceVo -> {
                    // 获取到设备的iotid
                    String iotId = deviceVo.getIotId();
                    // 从Redis中查询这个设备最近一次上报的数据
                    String jsonStr = (String) redisTemplate.opsForHash().get(CacheConstants.IOT_DEVICE_LAST_DATA, iotId);
                    if (StringUtils.isEmpty(jsonStr)) {
                        return;     // 跳出本次循环，并不是结束方法
                    }
                    deviceVo.setDeviceDataVos(JSONUtil.toList(jsonStr, DeviceData.class));
                });
            });
        });
        return roomVos;
    }
}
