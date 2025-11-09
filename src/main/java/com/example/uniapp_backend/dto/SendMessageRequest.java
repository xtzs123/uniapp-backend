package com.example.uniapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "联系人ID不能为空")
    private Long contactId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private Integer messageType = 1; // 1-文本, 2-图片, 3-语音, 4-文件

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }
}