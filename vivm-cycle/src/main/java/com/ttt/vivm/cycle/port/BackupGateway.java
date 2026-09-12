package com.ttt.vivm.cycle.port;

public interface BackupGateway {

    void createBackup(String poolName, String versionName);

    void restoreBackup(String poolName, String versionName);
}
