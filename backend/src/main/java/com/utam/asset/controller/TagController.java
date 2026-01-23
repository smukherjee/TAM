package com.utam.asset.controller;

import org.springframework.web.bind.annotation.*;
import com.utam.common.ApiResponse;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @GetMapping
    public ApiResponse<List<Object>> getAllTags(@RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        String tenantCode = icaoCode != null ? icaoCode : "VIDP";
        // TODO: Implement tags repository and service
        return ApiResponse.success(Collections.emptyList());
    }
}
