package com.ttt.vivm.connector.real;

import java.time.Duration;

public record SshCommandResult(String stdout, String stderr, int exitCode, Duration duration) {
}
