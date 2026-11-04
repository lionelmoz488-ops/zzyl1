package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.IdcardUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.CodeGenerator;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.domain.*;
import com.zzyl.nursing.dto.CheckInApplyDto;
import com.zzyl.nursing.dto.CheckInElderDto;
import com.zzyl.nursing.mapper.*;
import com.zzyl.nursing.vo.CheckInConfigVo;
import com.zzyl.nursing.vo.CheckInDetailVo;
import com.zzyl.nursing.vo.CheckInElderVo;
import com.zzyl.nursing.vo.ElderFamilyVo;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.service.ICheckInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

/**
 * 入住Service业务层处理
 * 
 * @author pcj
 * @date 2026-11-02
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService
{
    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private ElderMapper elderMapper;

    @Autowired
    private BedMapper bedMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private CheckInConfigMapper checkInConfigMapper;
    /**
     * 查询入住
     * 
     * @param id 入住主键
     * @return 入住
     */
    @Override
    public CheckIn selectCheckInById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询入住列表
     * 
     * @param checkIn 入住
     * @return 入住
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn)
    {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住
     * 
     * @param checkIn 入住
     * @return 结果
     */
    @Override
    public int insertCheckIn(CheckIn checkIn)
    {
        return save(checkIn) ? 1 : 0;
    }

    /**
     * 修改入住
     * 
     * @param checkIn 入住
     * @return 结果
     */
    @Override
    public int updateCheckIn(CheckIn checkIn)
    {
        return updateById(checkIn) ? 1 : 0;
    }

    /**
     * 批量删除入住
     * 
     * @param ids 需要删除的入住主键
     * @return 结果
     */
    @Override
    public int deleteCheckInByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除入住信息
     * 
     * @param id 入住主键
     * @return 结果
     */
    @Override
    public int deleteCheckInById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 申请入住
     *
     * @param checkInApplyDto 申请入住对象
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(CheckInApplyDto checkInApplyDto) {
        // 校验老人是否已入住，如果已入住，抛出异常
        LambdaQueryWrapper<Elder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Elder::getId, checkInApplyDto.getCheckInElderDto().getIdCardNo())
                        .in(Elder::getStatus, 1, 4);
        Elder elder = elderMapper.selectOne(lambdaQueryWrapper);
        if (ObjectUtils.isNotEmpty(elder)) {
            throw new BaseException("老人已入住,请勿重复入住");
        }

        //更新床位状态为已入住
        Bed bed = bedMapper.selectById(checkInApplyDto.getCheckInConfigDto().getBedId());
        bed.setBedStatus(1);
        bedMapper.updateById(bed);

        //新增或者更新老人基本信息
        elder = insertOrUpdateElder(bed,checkInApplyDto.getCheckInElderDto());

        //生成合同编号
        String contractNumber ="HT" + CodeGenerator.generateContractNumber();

        //新增签约办理信息
        insertContract(elder, contractNumber, checkInApplyDto);

        //新增入住信息
        CheckIn checkIn = insertCheckInInfo(elder, checkInApplyDto);


        //新增入住配置信息
        insertCheckInConfig(checkIn.getId(), checkInApplyDto);
    }

    /**
     * 新增入住配置信息
     *
     * @param id 入住主键
     * @param checkInApplyDto 申请入住对象
     */
    private void insertCheckInConfig(Long id, CheckInApplyDto checkInApplyDto) {
        CheckInConfig checkInConfig = new CheckInConfig();
        checkInConfig.setCheckInId(id);
        BeanUtils.copyProperties(checkInApplyDto.getCheckInConfigDto(), checkInConfig);
        checkInConfigMapper.insert(checkInConfig);
    }

    /**
     * 新增入住信息
     *
     * @param elder 老人信息
     * @param checkInApplyDto 申请入住对象
     */
    private CheckIn insertCheckInInfo(Elder elder, CheckInApplyDto checkInApplyDto) {
        CheckIn checkIn = new CheckIn();
        checkIn.setElderId(elder.getId());
        checkIn.setElderName(elder.getName());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setStartDate(checkInApplyDto.getCheckInConfigDto().getStartDate());
        checkIn.setEndDate(checkInApplyDto.getCheckInConfigDto().getEndDate());
        checkIn.setNursingLevelName(checkInApplyDto.getCheckInConfigDto().getNursingLevelName());
        checkIn.setBedNumber(elder.getBedNumber());
        checkIn.setStatus(0);
        checkIn.setRemark(JSON.toJSONString(checkInApplyDto.getElderFamilyDtoList()));

        checkInMapper.insert(checkIn);
        return checkIn;
    }

    /**
     * 新增签约办理信息
     *
     * @param elder 老人信息
     * @param contractNo 合同编号
     * @param checkInApplyDto 申请入住对象
     */
    private void insertContract(Elder elder, String contractNo, CheckInApplyDto checkInApplyDto) {
        Contract contract = new Contract();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInContractDto(), contract);
        contract.setElderId(elder.getId());
        contract.setContractNumber(contractNo);
        contract.setElderName(elder.getName());

        // 获取入住开始时间和结束时间
        LocalDateTime startDate = checkInApplyDto.getCheckInConfigDto().getStartDate();
        LocalDateTime endDate = checkInApplyDto.getCheckInConfigDto().getEndDate();
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        int status = startDate.isAfter(LocalDateTime.now()) ? 0 : 1;
        contract.setStatus(status);
        contractMapper.insert(contract);
    }

    /**
     * 新增或者更新老人基本信息
     *
     * @param bed 床位信息
     * @param checkInElderDto 老人信息
     */
    private Elder insertOrUpdateElder(Bed bed, CheckInElderDto checkInElderDto) {
        Elder elder = new Elder();
        BeanUtils.copyProperties(checkInElderDto, elder);
        elder.setBedId(bed.getId());
        elder.setBedNumber(bed.getBedNumber());
        elder.setStatus(1);

        //判断应该新增还是更新
        LambdaQueryWrapper<Elder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Elder::getIdCardNo, checkInElderDto.getIdCardNo());
        lambdaQueryWrapper.notIn(Elder::getStatus, 1,4);
        Elder elderInDb = elderMapper.selectOne(lambdaQueryWrapper);
        if (ObjectUtils.isNotEmpty(elderInDb)){
            elderMapper.updateById(elder);
        } else {
            elderMapper.insert(elder);
        }
        return elder;
    }

    /**
     * 获取入住详情
     *
     * @param id 入住主键
     * @return 入住详情
     */
    @Override
    public CheckInDetailVo getDetail(Long id) {

        CheckInDetailVo checkInDetailVo = new CheckInDetailVo();

        //设置入住配置响应信息
        CheckInConfigVo checkInConfigVo = new CheckInConfigVo();
        CheckIn checkIn = checkInMapper.selectById(id);
        BeanUtils.copyProperties(checkIn, checkInConfigVo);

        CheckInConfig checkInConfig = checkInConfigMapper.selectOne(new LambdaQueryWrapper<CheckInConfig>().eq(CheckInConfig::getCheckInId, id));
        BeanUtils.copyProperties(checkInConfig, checkInConfigVo);

        checkInDetailVo.setCheckInConfigVo(checkInConfigVo);

        //设置老人响应信息
        CheckInElderVo checkInElderVo = new CheckInElderVo();
        Elder elder = elderMapper.selectById(checkIn.getElderId());
        BeanUtils.copyProperties(elder, checkInElderVo);
        checkInElderVo.setAge(IdcardUtil.getAgeByIdCard(elder.getIdCardNo()));
        checkInDetailVo.setCheckInElderVo(checkInElderVo);

        //设置家属响应信息
        String remark = checkIn.getRemark();
        List<ElderFamilyVo> elderFamilyList = JSON.parseArray(remark, ElderFamilyVo.class);
        checkInDetailVo.setElderFamilyVoList(elderFamilyList);

        //设置签约办理响应信息
        Contract contract = contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getElderId, checkIn.getElderId()));
        checkInDetailVo.setContract(contract);
        return checkInDetailVo;
    }
}
