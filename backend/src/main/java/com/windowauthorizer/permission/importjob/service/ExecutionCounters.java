package com.windowauthorizer.permission.importjob.service;

import com.windowauthorizer.permission.importjob.domain.ExecutionAction;
import com.windowauthorizer.permission.importjob.engine.EngineCommandResult;

public record ExecutionCounters(long add, long update, long remove, long skip, long failed) {
    public static ExecutionCounters empty() {
        return new ExecutionCounters(0, 0, 0, 0, 0);
    }

    public ExecutionCounters add(EngineCommandResult result) {
        long newFailed = failed + (result.success() ? 0 : 1);
        if (!result.success()) {
            return new ExecutionCounters(add, update, remove, skip, newFailed);
        }
        return switch (result.action()) {
            case ADD -> new ExecutionCounters(add + 1, update, remove, skip, newFailed);
            case UPDATE -> new ExecutionCounters(add, update + 1, remove, skip, newFailed);
            case REMOVE -> new ExecutionCounters(add, update, remove + 1, skip, newFailed);
            case SKIP -> new ExecutionCounters(add, update, remove, skip + 1, newFailed);
        };
    }

    public ExecutionCounters addAll(Iterable<EngineCommandResult> results) {
        ExecutionCounters counters = this;
        for (EngineCommandResult result : results) {
            counters = counters.add(result);
        }
        return counters;
    }

    public long processed() {
        return add + update + remove + skip + failed;
    }
}
