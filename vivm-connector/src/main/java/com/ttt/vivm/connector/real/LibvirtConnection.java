package com.ttt.vivm.connector.real;

import com.ttt.vivm.cycle.port.ExternalOperationException;
import jakarta.annotation.PreDestroy;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
final class LibvirtConnection {

    private final Connect connection;

    LibvirtConnection(@Value("${libvirt.uri}") String uri) {
        try {
            this.connection = new Connect(uri);
        } catch (LibvirtException error) {
            throw new ExternalOperationException(
                    "CONNECT_LIBVIRT",
                    ExternalOperationException.Outcome.FAILED,
                    "Unable to connect to libvirt",
                    error);
        }
    }

    Connect get() {
        return connection;
    }

    @PreDestroy
    void close() throws LibvirtException {
        connection.close();
    }
}
