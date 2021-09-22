package cn.x5456.rs.server.controller.preview.onlyoffice.dto;

import cn.x5456.rs.server.controller.preview.onlyoffice.enums.ModeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Date: 2020/10/29 13:59 <br/>
 * Author: chenyp@dist.com.cn <br/>
 * Desc:
 */
@Data
public class PermissionsDTO implements Serializable {

    @ApiModelProperty("编辑")
    private boolean edit = false;
    @ApiModelProperty("评论")
    private boolean comment = false;
    @ApiModelProperty("打印")
    private boolean print = false;
    @ApiModelProperty("下载")
    private boolean download = false;
    @ApiModelProperty("审查")
    private boolean review = false;
    @ApiModelProperty("复制")
    private boolean copy = false;
    @ApiModelProperty("填充表单")
    private boolean fillForms = false;
    @ApiModelProperty("允许过滤")
    private boolean modifyFilter = false;
    @ApiModelProperty("修改内容控件")
    private boolean modifyContentControl = false;

    public PermissionsDTO(ModeEnum mode){
        if (mode == ModeEnum.EDIT){
            this.edit = true;
            this.comment = true;
            this.print = true;
            this.download = true;
            this.review = true;
            this.copy = true;
            this.fillForms = true;
            this.modifyFilter = true;
            this.modifyContentControl = true;
        }
    }

}