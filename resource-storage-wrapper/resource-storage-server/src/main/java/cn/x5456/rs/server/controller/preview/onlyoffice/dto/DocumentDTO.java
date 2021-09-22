package cn.x5456.rs.server.controller.preview.onlyoffice.dto;

import cn.hutool.core.io.FileUtil;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.ModeEnum;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.OfficeTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.io.Serializable;
import java.net.URI;

/**
 * Date: 2020/10/29 13:54 <br/>
 * Author: chenyp@dist.com.cn <br/>
 * Desc:
 */
@Data
public class DocumentDTO implements Serializable {

    @ApiModelProperty("文档标识")
    private String key;
    @ApiModelProperty("文件类型")
    private String fileType;
    @ApiModelProperty("文件名称")
    private String title;
    @ApiModelProperty("文件链接")
    private String url;
    @ApiModelProperty("权限")
    private PermissionsDTO permissions;

    public DocumentDTO(ResourceInfo resourceInfo, ModeEnum modeEnum, OfficeTypeEnum officeTypeEnum, ServerHttpRequest request) {
        this.key = resourceInfo.getId();
        this.title = resourceInfo.getFileName();
        URI uri = request.getURI();
        this.url = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/rest/rs/v2/files/" + resourceInfo.getId();
        // TODO: 2021/9/22 在 resourceInfo 中加一个 fileType 属性
        this.fileType = FileUtil.extName(resourceInfo.getFileName());
        this.permissions = new PermissionsDTO(modeEnum);
    }

//    public DocumentDTO(String type, String url){
//        this.key = Base64.getEncoder().encodeToString(url.getBytes());
//        this.title = url;
//        this.url = url;
//        if (FileUtil.getOfficeType(type) == OfficeTypeEnum.text && !FileUtil.ExtsDocument.contains("."+type)){
//            this.fileType = "txt";
//        }else{
//            this.fileType = type;
//        }
//        this.permissions = new PermissionsDTO(ModeEnum.view);
//    }

}