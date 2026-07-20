package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzyl.common.utils.CodeGenerator;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.domain.*;
import com.zzyl.nursing.dto.CheckInApplyDto;
import com.zzyl.nursing.mapper.*;
import com.zzyl.nursing.vo.CheckInConfigVo;
import com.zzyl.nursing.vo.CheckInDetailVo;
import com.zzyl.nursing.vo.CheckInElderVo;
import com.zzyl.nursing.vo.ElderFamilyVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.service.ICheckInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

/**
 * 入住Service业务层处理
 * 
 * @author alexis
 * @date 2026-07-17
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
     * 入住申请
     * @param checkInApplyDto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(CheckInApplyDto checkInApplyDto) {
        //1.先判断老人是否入住
        LambdaQueryWrapper<Elder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Elder::getIdCardNo, checkInApplyDto.getCheckInElderDto().getIdCardNo());
        queryWrapper.eq(Elder::getStatus, 1);
        Elder elder = elderMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(elder)){
            throw new RuntimeException("该老人已入住");
        }

        //2.更新床位状态为已入住（避免其他用户同时入住）
        LambdaQueryWrapper<Bed> BedQueryWrapper = new LambdaQueryWrapper<>();
        BedQueryWrapper.eq(Bed::getId, checkInApplyDto.getCheckInConfigDto().getBedId());
        Bed bed = bedMapper.selectOne(BedQueryWrapper);
        bed.setBedStatus(1);
        bedMapper.update(bed, BedQueryWrapper);


        //3.新增或更新老人（因为老人可能之前在养老院住过，所以可能是更新）
        elder = insertOrUpdate(bed, checkInApplyDto);

        //4.新增签约办理
        //设置合同名字，合同编号
        String contractName = elder.getName() + "的合同";
        String contractNumber = CodeGenerator.generateContractNumber();
        insertContract(elder,contractName,contractNumber,checkInApplyDto);

        //5.新增入住信息
        CheckIn checkIn = customInsertCheckIn(elder, bed, checkInApplyDto);

        //6.新增入住配置
        customInsertCheckInConfig(checkIn, checkInApplyDto);
    }

    //新增入住配置
    private void customInsertCheckInConfig(CheckIn checkIn, CheckInApplyDto checkInApplyDto) {
        CheckInConfig checkInConfig = new CheckInConfig();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInConfigDto(), checkInConfig);
        checkInConfig.setCheckInId(checkIn.getId());
        checkInConfigMapper.insert(checkInConfig);
    }

    //新增入住信息
    private CheckIn customInsertCheckIn(Elder elder, Bed bed, CheckInApplyDto checkInApplyDto) {
        CheckIn checkIn = new CheckIn();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInConfigDto(), checkIn);
        checkIn.setElderId(elder.getId());
        checkIn.setElderName(elder.getName());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setBedNumber(bed.getBedNumber());
        checkIn.setStatus(0);
        checkIn.setRemark(JSON.toJSONString(checkInApplyDto.getElderFamilyDtoList()));
        checkInMapper.insert(checkIn);
        return checkIn;
    }

    //新增合同
    private void insertContract(Elder elder, String contractName, String contractNumber, CheckInApplyDto checkInApplyDto) {
        Contract contract = new Contract();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInContractDto(), contract);
        contract.setContractName(contractName);
        contract.setContractNumber(contractNumber);
        contract.setElderId(elder.getId());
        contract.setElderName(elder.getName());

        //可以把入住时间，和离开时间当作合同的开始时间和结束日期
        //如果合同开始时间在当前时间之前，则合同状态为已生效
        LocalDateTime startDate = checkInApplyDto.getCheckInConfigDto().getStartDate();
        LocalDateTime endDate = checkInApplyDto.getCheckInConfigDto().getEndDate();
        if (startDate.isBefore(LocalDateTime.now())){
            contract.setStatus(1);
        }else {
            contract.setStatus(0);
        }
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contractMapper.insert(contract);
    }

    //新增或更新老人
    private Elder insertOrUpdate(Bed bed, CheckInApplyDto checkInApplyDto) {
        Elder elder = new Elder();
        BeanUtils.copyProperties(checkInApplyDto.getCheckInElderDto(), elder);
        elder.setBedId(bed.getId());
        elder.setBedNumber(bed.getBedNumber());
        elder.setStatus(1);
        LambdaQueryWrapper<Elder> ElderQueryWrapper = new LambdaQueryWrapper<>();
        ElderQueryWrapper.eq(Elder::getIdCardNo, checkInApplyDto.getCheckInElderDto().getIdCardNo());
        Elder elderDB = elderMapper.selectOne(ElderQueryWrapper);
        if (ObjectUtil.isEmpty(elderDB)){
            //新增老人
            elderMapper.insert(elder);
        }else{
            //更新老人
            elderMapper.updateById(elder);
        }
        return elder;
    }


    //查询入住老人详细信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInDetailVo detail(Long id) {
        //先判断老人是否入住
        CheckIn check = baseMapper.selectCheckInById(id);
        LambdaQueryWrapper<Elder> elderQueryWrapper = new LambdaQueryWrapper<>();
        elderQueryWrapper.eq(Elder::getId, check.getElderId());
        elderQueryWrapper.eq(Elder::getStatus, 1);
        Elder elder = elderMapper.selectOne(elderQueryWrapper);
        if (ObjectUtil.isEmpty(elder)){
            throw new RuntimeException("您还未入住");
        }

        CheckInDetailVo checkInDetailVo = new CheckInDetailVo();

        //查询老人信息
        CheckInElderVo checkInElderVo = new CheckInElderVo();
        BeanUtils.copyProperties(elder, checkInElderVo);
        checkInElderVo.setAge(IdcardUtil.getAgeByIdCard(elder.getIdCardNo()));
        checkInDetailVo.setCheckInElderVo(checkInElderVo);

        //查询老人家属信息
        LambdaQueryWrapper<CheckIn> checkInQueryWrapper = new LambdaQueryWrapper<>();
        checkInQueryWrapper.eq(CheckIn::getElderId,check.getElderId());
        CheckIn checkIn = checkInMapper.selectOne(checkInQueryWrapper);
        List<ElderFamilyVo> familyList = null;
        if (StrUtil.isNotBlank(checkIn.getRemark())) {
            familyList = JSONUtil.toList(checkIn.getRemark(), ElderFamilyVo.class);
        }
        checkInDetailVo.setElderFamilyVoList(familyList);


        //查询入住配置信息
        CheckInConfigVo checkInConfigVo = new CheckInConfigVo();
        LambdaQueryWrapper<CheckInConfig> checkInConfigQueryWrapper = new LambdaQueryWrapper<>();
        checkInConfigQueryWrapper.eq(CheckInConfig::getCheckInId, checkIn.getId());
        CheckInConfig checkInConfig = checkInConfigMapper.selectOne(checkInConfigQueryWrapper);
        if (ObjectUtil.isNotEmpty(checkInConfig)) {
            BeanUtils.copyProperties(checkInConfig, checkInConfigVo);
        }
        // 赋值给vo，解决空指针
        checkInDetailVo.setCheckInConfigVo(checkInConfigVo);
        checkInDetailVo.getCheckInConfigVo().setBedNumber(checkIn.getBedNumber());
        checkInDetailVo.getCheckInConfigVo().setStartDate(checkIn.getStartDate());
        checkInDetailVo.getCheckInConfigVo().setEndDate(checkIn.getEndDate());

        //查询合同信息
        LambdaQueryWrapper<Contract> contractQueryWrapper = new LambdaQueryWrapper<>();
        contractQueryWrapper.eq(Contract::getElderId, checkIn.getElderId());
        Contract contract = contractMapper.selectOne(contractQueryWrapper);
        if (ObjectUtil.isNotEmpty(contract)) {
            checkInDetailVo.setContract(contract);
        }

        return checkInDetailVo;
    }
}
