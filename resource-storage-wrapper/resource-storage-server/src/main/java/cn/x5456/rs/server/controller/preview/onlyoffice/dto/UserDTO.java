package cn.x5456.rs.server.controller.preview.onlyoffice.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Date: 2020/11/27 14:56 <br/>
 * Author: chenyp@dist.com.cn <br/>
 * Desc:
 */
@Data
public class UserDTO implements Serializable {

    @ApiModelProperty("用户标识")
    private String id = "dist";

    @ApiModelProperty("用户名称")
    private String name = "上海数慧";
}
