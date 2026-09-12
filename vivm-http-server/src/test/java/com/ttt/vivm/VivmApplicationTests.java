package com.ttt.vivm;

import com.ttt.vivm.connector.FakeExternalEnvironment;
import com.ttt.vivm.cycle.port.BackupGateway;
import com.ttt.vivm.cycle.port.ImageStorageGateway;
import com.ttt.vivm.cycle.port.VirtualizationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VivmApplicationTests {

	@Autowired
	private ImageStorageGateway imageStorageGateway;
	@Autowired
	private VirtualizationGateway virtualizationGateway;
	@Autowired
	private BackupGateway backupGateway;

	@Test
	void testProfileUsesOneStatefulFakeWithoutExternalConnections() {
		assertThat(imageStorageGateway).isInstanceOf(FakeExternalEnvironment.class);
		assertThat(virtualizationGateway).isSameAs(imageStorageGateway);
		assertThat(backupGateway).isSameAs(imageStorageGateway);
	}

}
