package com.ttt.vivm.service;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

@Service
public class SshService implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(SshService.class);

    @Value("${ssh.host}")
    private String host;

    @Value("${ssh.port:22}")
    private int port;

    @Value("${ssh.username}")
    private String username;

    @Value("${ssh.password:#{null}}")
    private String password;

    @Value("${ssh.privateKeyPath:#{null}}")
    private String privateKeyPath;

    @Value("${ssh.passphrase:#{null}}")
    private String passphrase;

    @Value("${ssh.connectionTimeout:30000}")
    private int connectionTimeout;

    @Value("${ssh.session.keepAliveInterval:60000}")
    private int keepAliveInterval;

    // 保存当前活跃的会话
    private Session session;

    // 标记服务是否已初始化
    private boolean initialized = false;

    /**
     * 服务初始化时建立SSH连接 (Spring的InitializingBean接口方法)
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 建立初始连接
            connect();
            initialized = true;
        } catch (Exception e) {
            logger.error("初始化SSH连接失败", e);
            // 初始连接失败不阻止服务启动
        }
    }

    /**
     * 服务销毁时关闭所有连接 (Spring的DisposableBean接口方法)
     */
    @Override
    public void destroy() {
        disconnect();
    }

    /**
     * 建立SSH连接
     */
    public synchronized void connect() throws JSchException {
        if (session != null && session.isConnected()) {
            return; // 已连接，无需重新连接
        }

        JSch jsch = new JSch();

        if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
            // 使用密钥认证
            if (passphrase != null && !passphrase.isEmpty()) {
                jsch.addIdentity(privateKeyPath, passphrase);
            } else {
                jsch.addIdentity(privateKeyPath);
            }
        }

        // 创建会话
        session = jsch.getSession(username, host, port);

        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }

        // 配置SSH连接
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        // 设置保持连接的配置
        session.setServerAliveInterval(keepAliveInterval);

        // 连接
        session.connect(connectionTimeout);
        logger.info("成功连接到SSH服务器: {}:{}", host, port);
    }

    /**
     * 断开SSH连接
     */
    public synchronized void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            logger.info("已断开SSH连接: {}:{}", host, port);
        }
    }

    /**
     * 检查会话是否已连接，如未连接则尝试重连
     */
    private synchronized void ensureConnected() throws JSchException {
        if (session == null || !session.isConnected()) {
            logger.info("SSH会话未连接，尝试重新连接...");
            connect();
        }
    }

    /**
     * 执行命令
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public String executeCommand(String command) {
        StringBuilder result = new StringBuilder();
        ChannelExec channel = null;

        try {
            // 确保连接已建立
            ensureConnected();

            // 打开执行命令的通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // 获取命令输出
            InputStream in = channel.getInputStream();
            channel.connect();

            // 读取命令输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            // 等待命令执行完成
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 获取退出状态
            int exitStatus = channel.getExitStatus();
            if (exitStatus != 0) {
                logger.warn("命令执行退出状态: {}, 命令: {}", exitStatus, command);
            }

        } catch (JSchException e) {
            logger.error("SSH连接失败", e);
            // 发生异常时，尝试重置连接状态
            disconnect();
            return "SSH连接失败: " + e.getMessage();
        } catch (IOException e) {
            logger.error("执行命令失败", e);
            return "执行命令失败: " + e.getMessage();
        } finally {
            // 关闭通道
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }

        return result.toString();
    }

    /**
     * 执行脚本
     * @param scriptPath 脚本在远程服务器上的路径
     * @return 脚本执行结果
     */
    public String executeScript(String scriptPath, String... args) {
        // 拼接所有参数，空格分隔，注意对每个参数加上引号避免空格或特殊字符出错
        StringBuilder fullCommand = new StringBuilder("chmod +x " + scriptPath + " && sudo " + scriptPath);

        for (String arg : args) {
            fullCommand.append(" ").append("\"").append(arg).append("\"");
        }
        System.out.println("Command: " + fullCommand);
        return executeCommand(fullCommand.toString());
    }

    /**
     * 检查连接状态
     * @return 如果连接活跃返回true
     */
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
}