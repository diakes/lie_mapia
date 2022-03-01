package com.lie.websocketinterface.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutboundToServiceMessageDto {
    private String sessionId;
    private String data;
}
