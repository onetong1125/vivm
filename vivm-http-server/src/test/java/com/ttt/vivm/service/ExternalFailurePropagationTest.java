package com.ttt.vivm.service;

import com.ttt.vivm.cycle.port.BackupGateway;
import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.repository.ImgRepository;
import com.ttt.vivm.repository.VersionImgRepository;
import com.ttt.vivm.repository.VersionPoolRepository;
import com.ttt.vivm.repository.VersionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ExternalFailurePropagationTest {

    @Test
    void shouldNotSaveBackupMetadataWhenExternalCopyFails() {
        VersionRepository versionRepository = mock(VersionRepository.class);
        BackupGateway backupGateway = mock(BackupGateway.class);
        VersionService service = new VersionService(
                versionRepository,
                mock(VersionPoolRepository.class),
                mock(VersionImgRepository.class),
                mock(ImgRepository.class),
                mock(PoolService.class),
                backupGateway,
                mock(ImageService.class));
        ExternalOperationException failure = new ExternalOperationException(
                "CREATE_BACKUP",
                ExternalOperationException.Outcome.FAILED,
                "copy failed");
        org.mockito.Mockito.doThrow(failure)
                .when(backupGateway).createBackup("pool-a", "version-a");

        assertThatThrownBy(() -> service.createNewBackup("pool-a", "version-a", "description"))
                .isSameAs(failure);
        verifyNoInteractions(versionRepository);
    }
}
