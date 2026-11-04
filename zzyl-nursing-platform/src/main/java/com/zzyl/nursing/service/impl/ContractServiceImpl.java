package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzyl.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.ContractMapper;
import com.zzyl.nursing.domain.Contract;
import com.zzyl.nursing.service.IContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 合同Service业务层处理
 * 
 * @author pcj
 * @date 2026-11-02
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    /**
     * 查询合同
     * 
     * @param id 合同主键
     * @return 合同
     */
    @Override
    public Contract selectContractById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询合同列表
     * 
     * @param contract 合同
     * @return 合同
     */
    @Override
    public List<Contract> selectContractList(Contract contract)
    {
        return contractMapper.selectContractList(contract);
    }

    /**
     * 新增合同
     * 
     * @param contract 合同
     * @return 结果
     */
    @Override
    public int insertContract(Contract contract)
    {
        return save(contract) ? 1 : 0;
    }

    /**
     * 修改合同
     * 
     * @param contract 合同
     * @return 结果
     */
    @Override
    public int updateContract(Contract contract)
    {
        return updateById(contract) ? 1 : 0;
    }

    /**
     * 批量删除合同
     * 
     * @param ids 需要删除的合同主键
     * @return 结果
     */
    @Override
    public int deleteContractByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除合同信息
     * 
     * @param id 合同主键
     * @return 结果
     */
    @Override
    public int deleteContractById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 更新合同状态
     *
     */
    @Override
    public void updateContractStatus() {
        //查询状态为0且开始时间小于等于当前时间且结束时间大于等于当前时间的合同
        List<Contract> list = list(Wrappers.<Contract>lambdaQuery()
                .eq(Contract::getStatus, 0)
                .le(Contract::getStartDate, LocalDateTime.now())
                .ge(Contract::getEndDate, LocalDateTime.now()));

        //更新合同状态为1
        list.forEach(contract -> contract.setStatus(1));

        updateBatchById(list);
    }
}
