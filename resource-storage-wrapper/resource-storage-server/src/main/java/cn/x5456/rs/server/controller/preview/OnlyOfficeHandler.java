package cn.x5456.rs.server.controller.preview;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.x5456.infrastructure.enums.EnumInterface;
import cn.x5456.rs.constant.AttachmentConstant;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.server.controller.preview.onlyoffice.FileModelDTO;
import cn.x5456.rs.server.controller.preview.onlyoffice.dto.UserDTO;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.ModeEnum;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.OfficeTypeEnum;
import cn.x5456.rs.server.controller.preview.onlyoffice.enums.PlatformEnum;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * @author yujx
 * @date 2021/09/17 15:29
 */
@Component
public class OnlyOfficeHandler implements FilePreviewHandler {

    /**
     * 支持的文本文件格式
     */
    private static final List<String> EXT_DOCUMENT = Arrays.asList(
            "doc", "docx", "docm",
            "dot", "dotx", "dotm",
            "odt", "fodt", "ott", "rtf", "txt",
            "js", "log", "md", "conf", "css", "java", "py", "xml", "yml", "yaml",
            "html", "htm", "mht",
            "pdf", "djvu", "fb2", "epub", "xps"
    );

    /**
     * 支持的Excel文件格式
     */
    private static final List<String> EXT_SPREADSHEET = Arrays.asList(
            "xls", "xlsx", "xlsm",
            "xlt", "xltx", "xltm",
            "ods", "fods", "ots", "csv"
    );

    /**
     * 支持的PPT文件格式
     */
    private static final List<String> EXT_PRESENTATION = Arrays.asList(
            "pps", "ppsx", "ppsm",
            "ppt", "pptx", "pptm",
            "pot", "potx", "potm",
            "odp", "fodp", "otp"
    );

    private static final List<String> ALL = CollUtil.unionAll(EXT_DOCUMENT, EXT_SPREADSHEET, EXT_PRESENTATION);

    @Autowired
    private IResourceStorage resourceStorage;

    @Override
    public boolean supports(ResourceInfo resourceInfo) {
        String fileType = getFileType(resourceInfo);
        return StrUtil.isNotBlank(fileType) && ALL.contains(fileType);
    }

    @Nullable
    private String getFileType(ResourceInfo resourceInfo) {
        return resourceStorage.getAttachment(resourceInfo.getId(), AttachmentConstant.FILE_TYPE, String.class).block();
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, ResourceInfo resourceInfo, String localFilePath, String platform, String mode) {
        Assert.isTrue(this.supports(resourceInfo), "未找到与当前文件类型匹配的处理器，无法预览！");

        // 拼接预览 url
        String url = StrUtil.format("/office.html?id={}&platform={}&mode={}", resourceInfo.getId(), platform, mode);
        response.getHeaders().setLocation(URI.create(url));
        return Mono.empty();
    }

    /**
     * 获取文档模型
     */
    public Mono<Object> getDocModel(ServerHttpRequest request, ResourceInfo resourceInfo, String platform, String mode) {
        Assert.isTrue(this.supports(resourceInfo), "未找到与当前文件类型匹配的处理器，无法预览！");

        PlatformEnum platformEnum = EnumInterface.getByDesc(PlatformEnum.class, platform);
        Assert.notNull(platformEnum, "传入的 platform 值不正确！");

        ModeEnum modeEnum = EnumInterface.getByDesc(ModeEnum.class, mode);
        Assert.notNull(modeEnum, "传入的 mode 值不正确！");

        OfficeTypeEnum officeTypeEnum = this.getOfficeType(resourceInfo);
        Assert.notNull(officeTypeEnum, "不可能报错~");

        // TODO: 2021/9/18 userDTO
        FileModelDTO fileModelDTO = new FileModelDTO(resourceInfo, platformEnum, modeEnum, officeTypeEnum, new UserDTO(), request);
        return Mono.just(fileModelDTO);
    }

    private OfficeTypeEnum getOfficeType(ResourceInfo resourceInfo) {
        OfficeTypeEnum officeTypeEnum = null;
        String fileType = this.getFileType(resourceInfo);
        if (EXT_DOCUMENT.contains(fileType)) {
            officeTypeEnum = OfficeTypeEnum.TEXT;
        } else if (EXT_SPREADSHEET.contains(fileType)) {
            officeTypeEnum = OfficeTypeEnum.SPREADSHEET;
        } else if (EXT_PRESENTATION.contains(fileType)) {
            officeTypeEnum = OfficeTypeEnum.PRESENTATION;
        }
        return officeTypeEnum;
    }
}
