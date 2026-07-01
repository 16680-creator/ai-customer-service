package com.aics.notify.controller;

import com.aics.common.result.Result;
import com.aics.notify.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@Tag(name = "通知管理")
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Validated
public class NotifyController {

    private final NotifyService notifyService;

    @Operation(summary = "发送通知给指定用户")
    @PostMapping("/send")
    public Result<Void> sendToUser(@RequestParam String userId,
                                    @RequestParam String message) {
        notifyService.sendToUser(userId, message);
        return Result.success();
    }

    @Operation(summary = "广播通知")
    @PostMapping("/broadcast")
    public Result<Void> broadcast(@RequestParam String message) {
        notifyService.broadcast(message);
        return Result.success();
    }

    @Operation(summary = "获取在线用户数")
    @GetMapping("/online")
    public Result<Integer> getOnlineCount() {
        int count = notifyService.getOnlineCount();
        return Result.success(count);
    }
}
