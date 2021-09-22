package cn.x5456.rs.server.controller.preview.onlyoffice;

import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.server.controller.preview.onlyoffice.dto.DocumentDTO;
import cn.x5456.rs.server.controller.preview.onlyoffice.dto.EditorConfigDTO;
import cn.x5456.rs.server.controller.preview.onlyoffice.dto.UserDTO;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.ModeEnum;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.OfficeTypeEnum;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.PlatformEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.io.Serializable;

@Data
public class FileModelDTO implements Serializable {

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("文档类型")
    private String documentType;

    @ApiModelProperty("文档元数据")
    private DocumentDTO document;

    @ApiModelProperty("编辑配置")
    private EditorConfigDTO editorConfig;

    public FileModelDTO(ResourceInfo resourceInfo, PlatformEnum platformEnum, ModeEnum modeEnum,
                        OfficeTypeEnum officeTypeEnum, UserDTO user, ServerHttpRequest request) {
        this.type = platformEnum.desc();
        this.documentType = officeTypeEnum.desc();

        ModeEnum temp = platformEnum != PlatformEnum.DESKTOP ? modeEnum : ModeEnum.VIEW;
        this.document = new DocumentDTO(resourceInfo, temp, officeTypeEnum, request);
        this.editorConfig = new EditorConfigDTO(resourceInfo, temp, user);
    }

//    public FileModelDTO(PlatformEnum platformEnum, String type, String url, BaseUser user){
//        this.type = platformEnum.name();
//        this.documentType = FileUtil.getOfficeType(type).name();
//        this.document = new DocumentDTO(type, url);
//        this.editorConfig = new EditorConfigDTO(ModeEnum.view, user);
//    }

}