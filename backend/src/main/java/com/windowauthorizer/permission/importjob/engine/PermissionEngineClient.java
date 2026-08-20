package com.windowauthorizer.permission.importjob.engine;

import java.util.List;

public interface PermissionEngineClient {
    boolean isConfigured();

    List<EngineCommandResult> execute(List<PermissionCommand> commands);
}
