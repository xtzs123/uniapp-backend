package com.example.uniapp_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleFriendRequest {
    @NotNull(message = "申请ID不能为空")
    private Long requestId;

    @NotNull(message = "处理结果不能为空")
    private Boolean accepted; // true-同意, false-拒绝

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }
}