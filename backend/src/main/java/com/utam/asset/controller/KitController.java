package com.utam.asset.controller;

import org.springframework.web.bind.annotation.*;
import com.utam.common.ApiResponse;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/kits")
public class KitController {

    @GetMapping
    public ApiResponse<List<Object>> getAllKits(@RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        // TODO: Use icaoCode for tenant filtering when implementing kits repository and service
        return ApiResponse.success(Collections.emptyList());
    }
}
