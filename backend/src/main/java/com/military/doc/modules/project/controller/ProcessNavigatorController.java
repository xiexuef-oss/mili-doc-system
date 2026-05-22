package com.military.doc.modules.project.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.project.service.ProcessNavigatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/navigator")
public class ProcessNavigatorController {

    @Autowired private ProcessNavigatorService navigatorService;

    @GetMapping
    public Result<Map<String, Object>> getNavigator(@PathVariable Long projectId) {
        return Result.success(navigatorService.getNavigatorData(projectId));
    }
}
