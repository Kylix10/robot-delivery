package com.example.robotdelivery.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BackendMonitorDaemon
 * 守护进程，用于监控 backend jar 是否运行，如果未运行则启动
 * 并在守护进程退出时尝试关闭后台服务
 */
public class BackendMonitorDaemon
{
    private volatile boolean running = true;

    // bat 脚本相对路径（相对于守护进程类）
    private static final String START_BAT_PATH = "start-backend.bat";
    private static final long CHECK_INTERVAL = 5000; // 5 秒
    private static final int BACKEND_PORT = 8088; // 后端服务端口
    private Process batProcess;

    public BackendMonitorDaemon()
    {
        // 注册 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            log("=== BackendMonitorDaemon 即将退出，检查是否需要关闭后台服务 ===");
            stopBackendByPort();
        }));
    }

    public void run()
    {
        log("=== BackendMonitorDaemon 启动 ===");

        while (running)
        {
            if (!isBackendRunning())
            {
                log("后台服务未运行，启动中...");
                startBackend();
                if (!waitForPortStartup(BACKEND_PORT, 15000)) // 最多等待 15 秒
                {
                    log("等待后台服务端口超时，请检查服务是否正常启动");
                }
                else
                {
                    log("后台服务启动完成，端口可访问");
                }
            }
            else
            {
                log("后台服务已在运行，跳过启动。");
            }

            try
            {
                Thread.sleep(CHECK_INTERVAL);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        log("=== BackendMonitorDaemon 已退出 ===");
    }

    /** 执行 bat 脚本启动后台服务 */
    private void startBackend()
    {
        try
        {
            batProcess = new ProcessBuilder("cmd.exe", "/c", START_BAT_PATH)
                    .redirectErrorStream(true)
                    .start();

            // 打印 bat 输出，便于调试
            new Thread(() ->
            {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(batProcess.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        log("[BAT] " + line);
                    }
                }
                catch (IOException ignored)
                {
                }
            }).start();

            log("启动脚本已执行，PID=" + batProcess.pid());

        }
        catch (IOException e)
        {
            log("启动脚本失败: " + e.getMessage());
        }
    }

    /** 检查后台服务是否已启动（通过端口检测） */
    private boolean isBackendRunning()
    {
        try (Socket socket = new Socket("localhost", BACKEND_PORT))
        {
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /** 等待端口可用，最长等待 timeout 毫秒 */
    private boolean waitForPortStartup(int port, long timeout)
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout)
        {
            if (isBackendRunning())
            {
                return true;
            }
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** 根据端口尝试关闭后台服务（Windows 下通过 taskkill 查找 java 进程） */
    private void stopBackendByPort()
    {
        try
        {
            // 查找占用端口的 PID
            Process find = new ProcessBuilder("cmd.exe", "/c",
                    "for /f \"tokens=5\" %a in ('netstat -ano ^| findstr :" + BACKEND_PORT + "') do @echo %a")
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(find.getInputStream()));
            String pid;
            while ((pid = reader.readLine()) != null)
            {
                pid = pid.trim();
                if (!pid.isEmpty())
                {
                    log("后台服务正在运行，尝试关闭 PID=" + pid);
                    new ProcessBuilder("cmd.exe", "/c", "taskkill /F /PID " + pid).start();
                }
            }
        }
        catch (IOException e)
        {
            log("关闭后台服务失败: " + e.getMessage());
        }
    }

    /** 日志输出，带时间戳 */
    private static void log(String msg)
    {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        System.out.println("[" + time + "] " + msg);
    }

    public static void main(String[] args)
    {
        new BackendMonitorDaemon().run();
    }
}
