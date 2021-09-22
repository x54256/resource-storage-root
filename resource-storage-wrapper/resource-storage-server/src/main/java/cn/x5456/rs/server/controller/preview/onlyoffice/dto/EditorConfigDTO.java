package cn.x5456.rs.server.controller.preview.onlyoffice.dto;

import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.ModeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Date: 2020/10/29 14:01 <br/>
 * Author: chenyp@dist.com.cn <br/>
 * Desc:
 */
@Data
public class EditorConfigDTO implements Serializable {

    @ApiModelProperty("模式")
    private String mode;
    @ApiModelProperty("回调地址")
    private String callbackUrl;
    @ApiModelProperty("显示语言")
    private String lang = "zh";
    @ApiModelProperty("所属区域")
    private String region = "zh-CN";
    @ApiModelProperty("定制内容")
    private CustomizationDTO customization;
    @ApiModelProperty("登录用户")
    private UserDTO user;

//    public EditorConfigDTO(ModeEnum modeEnum, BaseUser user){
//        this.mode = modeEnum.name();
//        this.customization = new CustomizationDTO(modeEnum);
//        this.user = new UserDTO(user);
//    }

    public EditorConfigDTO(ResourceInfo resourceInfo, ModeEnum modeEnum, UserDTO user) {
        this.mode = modeEnum.desc();
        this.user = user;
        this.customization = new CustomizationDTO(modeEnum);
        // TODO: 2021/9/18 edit
//        if (modeEnum == ModeEnum.EDIT) {
//            this.callbackUrl = CommonUtil.getServerUrl() + "/model/" + doc.getId();
//        }
    }

}