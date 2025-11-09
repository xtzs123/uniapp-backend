package com.example.uniapp_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContactDTO {
    private Long id;

    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    private String remarkName;

    private String contactGroup = "默认";

    private Integer status = 1;
}