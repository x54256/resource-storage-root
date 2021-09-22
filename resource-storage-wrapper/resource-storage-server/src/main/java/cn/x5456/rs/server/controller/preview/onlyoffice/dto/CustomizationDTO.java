package cn.x5456.rs.server.controller.preview.onlyoffice.dto;

import cn.x5456.rs.server.controller.preview.onlyoffice.enums.ModeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * Date: 2020/11/3 16:57 <br/>
 * Author: chenyp@dist.com.cn <br/>
 * Desc:
 */
@Data
public class CustomizationDTO implements Serializable {

    private boolean about = false;
    private boolean help = false;
    private boolean plugins = false;
    private boolean spellcheck = false;

    private boolean hideRightMenu = true;
    private Feedback feedback = new Feedback();

    private boolean chat = false;
    private boolean commentAuthorOnly = true;
    private boolean comments = false;

    private boolean toolbarNoTabs = true;
    private boolean compactHeader = true;
    private boolean compactToolbar = true;

    public CustomizationDTO(ModeEnum mode){
        if (mode == ModeEnum.EDIT){
            this.chat = true;
            this.commentAuthorOnly = false;
            this.comments = true;
            this.toolbarNoTabs = false;
            this.compactHeader = false;
            this.compactToolbar = false;
        }
    }

    @Data
    static class Feedback implements Serializable{
        private boolean visible = false;
    }

}