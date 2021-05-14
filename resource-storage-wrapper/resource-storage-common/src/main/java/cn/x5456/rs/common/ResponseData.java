package cn.x5456.rs.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Data
public final class ResponseData implements Serializable {

    @ApiModelProperty("标准HTTP状态码")
    private int status;

    @ApiModelProperty("数据")
    private Object data;

    @ApiModelProperty("响应消息，HTTP状态码status非200时代表错误消息")
    private String message;

    @ApiModelProperty("业务码，禁止代码中耦合，仅展示及后台排错用")
    private int code;

    private ResponseData(Object data) {
        this.status = HttpStatus.OK.value();
        this.code = 1;
        this.data = data;
        this.message = "success";
    }

    private ResponseData(Throwable e) {
        this.message = e.getMessage();
        if (e instanceof AbstractException) {
            AbstractException abstractException = (AbstractException) e;
            this.status = abstractException.getStatus().value();
            this.code = abstractException.getCode();
        } else {
            this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            this.code = -1;
        }

    }

    public static ResponseData success(Object data) {
        return new ResponseData(data);
    }

    public static ResponseData fail(Throwable e) {
        return new ResponseData(e);
    }
}