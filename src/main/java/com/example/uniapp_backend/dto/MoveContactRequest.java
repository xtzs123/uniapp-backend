package com.example.uniapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveContactRequest {
    @NotNull(message = "联系人ID不能为空")
    private Long contactId;

    @NotBlank(message = "目标分组不能为空")
    private String targetGroup;

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }
}