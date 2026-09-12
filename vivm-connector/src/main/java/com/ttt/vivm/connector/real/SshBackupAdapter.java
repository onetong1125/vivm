package com.ttt.vivm.connector.real;

import com.ttt.vivm.cycle.port.BackupGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
final class SshBackupAdapter implements BackupGateway {

    private final SshCommandExecutor ssh;
    private final String createScript;
    private final String restoreScript;

    SshBackupAdapter(
            SshCommandExecutor ssh,
            @Value("${backup.create-script}") String createScript,
            @Value("${backup.restore-script}") String restoreScript) {
        this.ssh = ssh;
        this.createScript = createScript;
        this.restoreScript = restoreScript;
    }

    @Override
    public void createBackup(String poolName, String versionName) {
        ssh.execute("CREATE_BACKUP", ssh.scriptCommand(createScript, poolName, versionName));
    }

    @Override
    public void restoreBackup(String poolName, String versionName) {
        ssh.execute("RESTORE_BACKUP", ssh.scriptCommand(restoreScript, poolName, versionName));
    }
}
