package com.example.robotdelivery.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

/**
 * 后端监控守护进程
 * <p>
 * 作用：
 * - 定时检查后端是否存活（通过健康检查接口）
 * - 后端崩溃或无响应时自动重启
 * - 可配置检测间隔和启动命令
 */

//待配置的内容，一个是这个进程得托管给Spring boot管理  一个是需要写一个cmd脚本来用于启动程序


public class BackendMonitorDaemon
{
    // 后端启动命令，需要Windows环境下执行
    private static final String[] BACKEND_START_COMMAND = {"cmd", "/c", "start-backend.bat"};

    // 后端健康检查URL
    private static final String BACKEND_HEALTH_URL = "http://localhost:8088/actuator/health";

    // 检查间隔，单位秒
    private static final int MONITOR_INTERVAL_SEC = 5;

    // 用于执行守护线程
    private ScheduledExecutorService executor;

    // 标记守护进程是否正在运行
    private volatile boolean running = false;

    /**
     * 启动守护进程
     */
    public void start()
    {
        if (running)
        {
            System.out.println("BackendMonitorDaemon 已经在运行");
            return;
        }

        running = true;
        executor = Executors.newSingleThreadScheduledExecutor();

        // 每隔 MONITOR_INTERVAL_SEC 秒执行一次
        executor.scheduleAtFixedRate(this::checkAndRestart, 0, MONITOR_INTERVAL_SEC, TimeUnit.SECONDS);

        System.out.println("BackendMonitorDaemon 启动成功");
    }

    /**
     * 停止守护进程
     */
    public void stop()
    {
        running = false;
        if (executor != null)
        {
            executor.shutdownNow();
            System.out.println("BackendMonitorDaemon 已停止");
        }
    }

    /**
     * 核心逻辑：
     * - 检查后端健康
     * - 不健康时执行重启
     */
    private void checkAndRestart()
    {
        try
        {
            if (!isBackendHealthy())
            {
                System.out.println("检测到后端异常，准备重启...");
                restartBackend();
            }
        }
        catch (Exception e)
        {
            System.err.println("BackendMonitorDaemon 检查异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 健康检查
     * @return true 表示后端正常，false 表示异常
     */
    private boolean isBackendHealthy()
    {
        try
        {
            URL url = new URL(BACKEND_HEALTH_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000); // 2秒超时
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * 重启后端
     */
    private void restartBackend()
    {
        try
        {
            // 执行Windows命令
            ProcessBuilder pb = new ProcessBuilder(BACKEND_START_COMMAND);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 输出启动日志
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    System.out.println("[Backend启动日志] " + line);
                }
            }

            System.out.println("后端已尝试重启");
        }
        catch (IOException e)
        {
            System.err.println("重启后端失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
