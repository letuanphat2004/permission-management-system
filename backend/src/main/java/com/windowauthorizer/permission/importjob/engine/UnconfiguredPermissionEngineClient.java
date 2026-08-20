package com.windowauthorizer.permission.importjob.engine;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnconfiguredPermissionEngineClient implements PermissionEngineClient {
    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public List<EngineCommandResult> execute(List<PermissionCommand> commands) {
        throw new EngineUnavailableException(
                "Permission Engine chưa được cấu hình. Không có thay đổi nào được ghi nhận là đã áp dụng vào AD."
        );
    }
}
