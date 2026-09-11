package com.ttt.vivm.connector;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class LibvirtConnector {
    private static final Logger logger = LoggerFactory.getLogger(LibvirtConnector.class);

    private Connect connection;

    @Value("${libvirt.uri}")
    private String libvirtUri;

    @PostConstruct
    public void init() {
        try {
            connection = new Connect(libvirtUri);
            logger.info("Libvirt connection established successfully: {}", libvirtUri);
        } catch (LibvirtException e) {
            logger.error("Failed to establish libvirt connection", e);
            throw new RuntimeException("Unable to connect to libvirt", e);
        }
    }

    public Connect getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Libvirt connection not initialized");
        }
        return connection;
    }

    @PreDestroy
    public void close() {
        try {
            if (connection != null) {
                connection.close();
                logger.info("Libvirt connection closed");
            }
        } catch (LibvirtException e) {
            logger.warn("Error closing libvirt connection", e);
        }
    }
}

