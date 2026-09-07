package com.appworks.portal.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<String> details;
}
