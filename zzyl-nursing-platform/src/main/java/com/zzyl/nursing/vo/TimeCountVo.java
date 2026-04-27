package com.zzyl.nursing.vo;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("分时段统计VO")
public class TimeCountVo {
    /**
     * 时间段
     */
    @ApiModelProperty(value = "时间段")
    private String time;

    /**
     * 剩余次数
     */
    @ApiModelProperty(value = "剩余次数")
    private String count;
}