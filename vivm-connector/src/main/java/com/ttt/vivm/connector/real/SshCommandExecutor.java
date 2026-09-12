package com.ttt.vivm.connector.real;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.ttt.vivm.cycle.port.ExternalOperationException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

@Component
@Profile("real")
final class SshCommandExecutor {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String privateKeyPath;
    private final String passphrase;
    private final String knownHostsPath;
    private final int connectionTimeoutMillis;
    private final int commandTimeoutMillis;
    private final int keepAliveIntervalMillis;

    private Session session;

    SshCommandExecutor(
            @Value("${ssh.host}") String host,
            @Value("${ssh.port:22}") int port,
            @Value("${ssh.username}") String username,
            @Value("${ssh.password:}") String password,
            @Value("${ssh.private-key-path:}") String privateKeyPath,
            @Value("${ssh.passphrase:}") String passphrase,
            @Value("${ssh.known-hosts-path}") String knownHostsPath,
            @Value("${ssh.connection-timeout-millis:30000}") int connectionTimeoutMillis,
            @Value("${ssh.command-timeout-millis:120000}") int commandTimeoutMillis,
            @Value("${ssh.keep-alive-interval-millis:60000}") int keepAliveIntervalMillis) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
        this.knownHostsPath = knownHostsPath;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.commandTimeoutMillis = commandTimeoutMillis;
        this.keepAliveIntervalMillis = keepAliveIntervalMillis;
    }

    SshCommandResult execute(String operation, String command) {
        ChannelExec channel = null;
        boolean commandStarted = false;
        Instant startedAt = Instant.now();
        try {
            Session activeSession = connectedSession();
            channel = (ChannelExec) activeSession.openChannel("exec");
            channel.setCommand(command);
            InputStream stdout = channel.getInputStream();
            InputStream stderr = channel.getExtInputStream();
            channel.connect(connectionTimeoutMillis);
            commandStarted = true;

            ByteArrayOutputStream stdoutBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBytes = new ByteArrayOutputStream();
            long deadline = System.nanoTime() + Duration.ofMillis(commandTimeoutMillis).toNanos();
            while (!channel.isClosed()) {
                drain(stdout, stdoutBytes);
                drain(stderr, stderrBytes);
                if (System.nanoTime() >= deadline) {
                    throw new ExternalOperationException(
                            operation,
                            ExternalOperationException.Outcome.UNKNOWN,
                            "SSH command timed out after " + commandTimeoutMillis + " ms");
                }
                Thread.sleep(20);
            }
            drain(stdout, stdoutBytes);
            drain(stderr, stderrBytes);

            SshCommandResult result = new SshCommandResult(
                    stdoutBytes.toString(StandardCharsets.UTF_8),
                    stderrBytes.toString(StandardCharsets.UTF_8),
                    channel.getExitStatus(),
                    Duration.between(startedAt, Instant.now()));
            if (result.exitCode() != 0) {
                throw new ExternalOperationException(
                        operation,
                        ExternalOperationException.Outcome.FAILED,
                        "SSH command exited with code " + result.exitCode() + ": " + result.stderr().trim());
            }
            return result;
        } catch (ExternalOperationException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ExternalOperationException(
                    operation,
                    commandStarted ? ExternalOperationException.Outcome.UNKNOWN
                            : ExternalOperationException.Outcome.FAILED,
                    "SSH command was interrupted",
                    error);
        } catch (JSchException | IOException error) {
            disconnect();
            throw new ExternalOperationException(
                    operation,
                    commandStarted ? ExternalOperationException.Outcome.UNKNOWN
                            : ExternalOperationException.Outcome.FAILED,
                    "SSH command failed: " + error.getMessage(),
                    error);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    String scriptCommand(String scriptPath, String... arguments) {
        StringBuilder command = new StringBuilder("sudo ").append(ShellArgument.quote(scriptPath));
        for (String argument : arguments) {
            command.append(' ').append(ShellArgument.quote(argument));
        }
        return command.toString();
    }

    private synchronized Session connectedSession() throws JSchException {
        if (session != null && session.isConnected()) {
            return session;
        }
        JSch jsch = new JSch();
        jsch.setKnownHosts(knownHostsPath);
        if (!privateKeyPath.isBlank()) {
            if (passphrase.isBlank()) {
                jsch.addIdentity(privateKeyPath);
            } else {
                jsch.addIdentity(privateKeyPath, passphrase);
            }
        }
        session = jsch.getSession(username, host, port);
        if (!password.isBlank()) {
            session.setPassword(password);
        }
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "yes");
        session.setConfig(config);
        session.setServerAliveInterval(keepAliveIntervalMillis);
        session.connect(connectionTimeoutMillis);
        return session;
    }

    private void drain(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        while (input.available() > 0) {
            int read = input.read(buffer, 0, Math.min(buffer.length, input.available()));
            if (read < 0) {
                return;
            }
            output.write(buffer, 0, read);
        }
    }

    @PreDestroy
    synchronized void disconnect() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }
}
