// 全局数据
let orders = [];
let robots = [];
let utensils = [];
let workstations = [];
let minuteCounter = 0; // 记录运行分钟数，用于图表横轴
let algorithmMetrics = {
    withAlgorithm: {
        avgResponseTime: 0,
        revenue: 0, // 吞吐量改为收益
        revenueHistory: [] // 新增：存储带算法的历史收益 [{time: 1, value: 100}, ...]
    },
    withoutAlgorithm: {
        avgResponseTime: 0,
        revenue: 0, // 吞吐量改为收益
        revenueHistory: [] // 新增：存储不带算法的历史收益
    }
};

const API_BASE = 'http://localhost:8088/api'; // 统一API前缀

// 内存管理相关数据和逻辑 (对应 MemoryVO 和 MemoryController)
let memoryManager = {
    // 仅保留获取状态的 API 地址，与后端 MemoryController 对应
    API_STATUS: `${API_BASE}/memory/status`,

    // 初始化方法只用于加载初始数据和设置定时刷新
    init: function() {
        this.fetchMemoryStatus(); // 首次加载状态
        // 设置定时刷新，用于持续监控
        setInterval(() => this.fetchMemoryStatus(), 1000); // 调整为1秒刷新一次
    },

    // ------------------- API 调用 (获取状态) -------------------

    // 获取内存状态 (对应 MemoryController.getMemoryStatus, 返回 MemoryVO)
    fetchMemoryStatus: async function() {
        try {
            const response = await fetch(this.API_STATUS);
            if (!response.ok) {
                // 打印 HTTP 错误状态
                throw new Error(`获取内存状态失败: HTTP ${response.status}`);
            }
            const data = await response.json(); // data 结构为 MemoryVO
            // 收到后端返回的 MemoryVO 数据后，更新可视化
            this.updateMemoryVisualization(data);
        } catch (error) {
            console.error("Fetch Memory Status Error:", error);
        }
    },

    // ------------------- 可视化更新 (基于 MemoryVO 数据) -------------------

    // 接收 MemoryVO 对象 (data) 并更新前端显示
    updateMemoryVisualization: function(data) {
        const container = document.getElementById('memory-container');
        const listContainer = document.getElementById('partitions-list');

        // 确保数据有效
        if (!data || !data.partitions || !container || !listContainer) return;

        const totalSpace = data.totalSpace; // 来自 MemoryVO
        const usedSpace = data.usedSpace;   // 来自 MemoryVO
        const freeSpace = data.freeSpace;   // 来自 MemoryVO
        const partitions = data.partitions; // 来自 MemoryVO

        // 计算碎片数量 (JS 端简化计算，空闲分区数量即为碎片数量)
        const freePartitions = partitions.filter(p => !p.allocated);
        const fragmentCount = freePartitions.length;
        const usageRate = (usedSpace / totalSpace) * 100;

        // 清空容器
        container.innerHTML = '';
        listContainer.innerHTML = '';

        // 1. 更新统计信息 (需要确保 HTML 元素存在)
        document.getElementById('total-memory').textContent = totalSpace;
        document.getElementById('used-memory').textContent = usedSpace;
        document.getElementById('free-memory').textContent = freeSpace;
        document.getElementById('fragment-count').textContent = fragmentCount;

        // 更新进度条
        const usageBar = document.getElementById('memory-usage-bar');
        if(usageBar){
            usageBar.style.width = `${usageRate}%`;
            usageBar.textContent = `已使用: ${Math.round(usageRate)}%`;
        }

        // 2. 生成分区可视化
        partitions.forEach(partition => {
            const partitionEl = document.createElement('div');
            partitionEl.className = `memory-partition ${partition.allocated ? 'allocated' : 'free'}`;

            // 计算宽度百分比
            const widthPercent = (partition.size / totalSpace) * 100; // Partition POJO 包含 size 字段
            partitionEl.style.width = `${widthPercent}%`;
            // 计算起始位置百分比 (假设 Partition POJO 有 start 字段)
            partitionEl.style.left = `${(partition.start / totalSpace) * 100}%`;

            // 设置显示文本
            if (partition.allocated) {
                // 使用 dishName 或 dishId 进行显示 (来自 Partition POJO)
                const dishLabel = partition.dishName || `ID:${partition.dishId}`;
                partitionEl.textContent = `${dishLabel} (${partition.size}KB)`;
            } else {
                partitionEl.textContent = `${partition.size}KB`;
            }

            container.appendChild(partitionEl);

            // 3. 添加到分区列表
            const listItem = document.createElement('div');
            listItem.className = `partition-item ${partition.allocated ? 'allocated' : 'free'}`;
            const dishInfo = partition.allocated ? `, 菜品: ${partition.dishName || `ID:${partition.dishId}`}` : '';
            // 假设 Partition POJO 包含 start 和 size 字段
            listItem.innerHTML = `
                <strong>${partition.allocated ? '已分配' : '空闲'}</strong>: 
                起始地址: ${partition.start}KB, 大小: ${partition.size}KB
                ${dishInfo}
            `;
            listContainer.appendChild(listItem);
        });
    }
};

// =================================================================
// 【数据获取函数】
// =================================================================
// *修改
// 从后端获取订单数据并渲染 (对应 OrderController 的 @GetMapping)
function fetchOrdersAndRender() {
    $.ajax({
        url: `${API_BASE}/orders`,
        method: 'GET',
        success: function(response) {
            // 后端返回的是ApiResponse对象，订单列表在response.data中
            if (response && response.code === 200 && Array.isArray(response.data)) {
                orders = response.data; // 正确获取订单列表
                renderOrders();
                renderDashboard(); // 更新仪表盘数据
            } else {
                // 处理接口返回格式错误的情况
                console.error("订单数据格式错误", response);
                orders = [];
                renderOrders();
                $('#no-orders-message').removeClass('d-none').html(`获取订单数据失败: ${response?.message || '未知错误'}`);
            }
        },
        error: function(xhr, status, error) {
            console.error("获取订单数据失败:", error);
            $('#no-orders-message').removeClass('d-none').html(`获取订单数据失败，请检查后端服务和接口路径。错误: <strong>${xhr.status} (${error})</strong>`);
        }
    });
}
// 从后端获取机器人数据并更新 (对应 RobotController.getAllRobots)
function fetchRobots() {
    $.ajax({
        url: `${API_BASE}/robots`,
        method: 'GET',
        success: function(data) {
            // data 是 List<RobotVO> 结构
            const newRobots = data.map(robotVO => {
                // 查找现有的机器人位置 (保留前端模拟的位置)
                const existingRobot = robots.find(r => r.id === 'RB' + robotVO.robotId);
                const statusText = robotVO.robotStatusDesc; // 状态描述
                // 0: 空闲, 1: 忙碌 (从状态描述反推前端状态码)
                const status = statusText === '空闲' ? 'idle' : 'busy';

                return {
                    id: 'RB' + robotVO.robotId,
                    name: '机器人' + robotVO.robotId,
                    status: status,
                    statusText: statusText,
                    battery: existingRobot ? existingRobot.battery : Math.floor(Math.random() * 40) + 60, // 保持模拟电池电量
                    currentTask: existingRobot ? existingRobot.currentTask : (status === 'busy' ? ('ORD' + (Math.floor(Math.random() * 8) + 1)) : null), // 保持模拟任务
                    // 保持前端模拟的位置，因为 RobotVO 只有 location ID
                    location: existingRobot ? existingRobot.location : { x: 50 + Math.random() * 600, y: 50 + Math.random() * 350 },
                    completedOrders: robotVO.finishedOrders || 0 // 已完成订单数
                };
            });
            robots = newRobots;
            renderRobots(); // 重新渲染机器人列表
        },
        error: function(xhr, status, error) {
            console.error("获取机器人数据失败:", error);
        }
    });
}

// 从后端获取器具数据并更新 (对应 ToolController.getAllToolStatus)
function fetchTools() {
    $.ajax({
        url: `${API_BASE}/tools/status`,
        method: 'GET',
        success: function(data) {
            // data 是 List<ToolVo> 结构
            utensils = data.map(toolVO => ({
                id: 'UT' + toolVO.toolId,
                type: toolVO.toolType, // OVEN, FRY_PAN 等
                status: toolVO.statusText === '空闲' ? 'available' : 'occupied', // 转换回前端状态码
                statusText: toolVO.statusText, // 空闲/占用中
                robotId: toolVO.occupiedByRobot // Robot-1 或 无
            }));
            renderResources(); // 重新渲染资源列表 (器具和工作台)
        },
        error: function(xhr, status, error) {
            console.error("获取器具数据失败:", error);
        }
    });
}

// 从后端获取工作台数据并更新 (对应 MemoryController.getWorkstationStatus)
function fetchWorkstations() {
    $.ajax({
        url: `${API_BASE}/memory/workstations`,
        method: 'GET',
        success: function(data) {
            // data 是 List<WorkstationVo> 结构
            workstations = data.map(wsVO => ({
                id: 'WS' + wsVO.id,
                capacity: wsVO.capacity,
                status: wsVO.status === '空闲' ? 'available' : 'occupied', // 转换回前端状态码
                statusText: wsVO.status, // 空闲/已分配
                robotId: wsVO.occupiedByRobot, // Robot-1 或 无
                currentTask: wsVO.currentTask // Dish-101 或 无
            }));
            renderResources(); // 重新渲染资源列表 (器具和工作台)
        },
        error: function(xhr, status, error) {
            console.error("获取工作台数据失败:", error);
        }
    });
}

// 页面加载完成后初始化
$(document).ready(function() {
    // 初始化导航切换
    initNavigation();

    // 初始化资源和地图 (移除模拟数据生成，保留地图初始化)
    initResourcesAndMap();

    // --- 【内存管理器初始化】 ---
    // 检查 #memory-page 元素是否存在，解决了点击内存导航无跳转的问题（如果 HTML 缺失元素）
    if (document.getElementById('memory-page')) {
        memoryManager.init();
    }

    // 绑定内存操作事件 (仅保留导航事件)
    bindMemoryEvents();

    // 初始获取所有数据并渲染
    fetchOrdersAndRender();
    fetchRobots();
    fetchTools();
    fetchWorkstations();

    // 渲染仪表盘
    renderDashboard();

    // 初始获取算法指标 (会调用图表渲染)

    fetchAlgorithmMetrics();


    // 设置定时刷新
    setInterval(function() {
        // 定期从后端获取最新状态数据
        fetchOrdersAndRender();
        fetchRobots();
        fetchTools();
        fetchWorkstations();

        // 模拟机器人位置更新（因为后端VO缺少坐标，无法实时获取，需保留此模拟逻辑）
        updateRobotPositions();

        // 重新渲染仪表盘
        renderDashboard();

        // 刷新算法指标（模拟）
        fetchAlgorithmMetrics();
    }, 10000); // 每5秒刷新一次
});

// 绑定内存操作事件 (移除内存分配/释放/整理，因为后端Controller未提供接口)
function bindMemoryEvents() {
    // 内存页面导航 (保留)
    $('#nav-memory').click(function(e) {
        e.preventDefault();
        showPage('memory-page');
        setActiveNav('nav-memory');
    });
}

// 显示操作消息
function showMemoryMessage(message, isSuccess) {
    const messageEl = document.createElement('div');
    messageEl.className = `alert ${isSuccess ? 'alert-success' : 'alert-danger'} position-fixed top-20 end-3 z-50`;
    messageEl.textContent = message;
    messageEl.style.maxWidth = '300px';

    document.body.appendChild(messageEl);

    setTimeout(() => {
        messageEl.classList.add('fade-out');
        setTimeout(() => {
            document.body.removeChild(messageEl);
        }, 500);
    }, 3000);
}

// 从后端获取算法指标数据
function fetchAlgorithmMetrics() {
    simulateAlgorithmMetrics();
    // 假设每 6 次（60秒/1分钟）更新一次历史数据
    if (minuteCounter % 6 === 0) {
        updateRevenueHistory(algorithmMetrics.withAlgorithm, 'withAlgorithm');
        updateRevenueHistory(algorithmMetrics.withoutAlgorithm, 'withoutAlgorithm');
    }
    minuteCounter++;
    updateAlgorithmMetricsUI();
}
// 假设已引入 jQuery 和 Chart.js
$(function() {
    const API_URL = "/api/performance/comparison";
    // 1 分钟 = 60000 毫秒。将后端返回的毫秒转换为分钟进行展示。
    const MS_TO_MINUTES = 60000;

    // 全局 Chart 实例，用于销毁和重绘
    let algRevenueChartInstance = null;
    let noAlgRevenueChartInstance = null;
    let comparisonChartInstance = null;

    /**
     * 渲染历史收益折线图
     * @param {string} chartId Canvas ID: 'alg-revenue-chart' 或 'noalg-revenue-chart'
     * @param {Array<Object>} historyData 后端返回的历史数据列表 (ModePerformanceDTO[])
     * @param {string} title 图表标题
     * @param {string} color 曲线颜色 (Bootstrap 样式中使用的颜色)
     */
    function renderLineChart(chartId, historyData, title, color) {
        const canvas = document.getElementById(chartId);
        if (!canvas) {
            console.error(`Canvas ID #${chartId} not found.`);
            return;
        }
        const ctx = canvas.getContext('2d');

        // 销毁旧图表实例
        let chartInstance = window[`${chartId}Instance`];
        if (chartInstance) {
            chartInstance.destroy();
        }

        // 提取数据点。x轴使用计算时间 (HH:mm:ss)，y轴使用总收益
        const labels = historyData.map(d => d.calcTime ? d.calcTime.substring(11, 19) : '');
        const data = historyData.map(d => d.totalRevenue);

        // 创建新图表实例并存储
        window[`${chartId}Instance`] = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: title,
                    data: data,
                    backgroundColor: color.replace('0.8', '0.5'),
                    borderColor: color.replace('0.8', '1'),
                    borderWidth: 2,
                    tension: 0.3, // 曲线平滑度
                    fill: false,
                    pointRadius: 4,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        // 调整 ticks 格式，使其支持大额显示 (K/M) <--- 【修改点开始】
                        title: { display: true, text: '累计总收益 (元)' },
                        ticks: {
                            callback: function(value) {
                                // 增加显示额度：当数值较大时，使用 K/M 等单位
                                if (value >= 1000000) {
                                    return '¥' + (value / 1000000).toFixed(1) + 'M';
                                }
                                if (value >= 1000) {
                                    return '¥' + (value / 1000).toFixed(1) + 'k';
                                }
                                return '¥' + value.toFixed(0); // 保持整数显示
                            }
                        }
                        // 【修改点结束】
                    },
                    x: {
                        title: { display: true, text: '时间 (HH:mm:ss)' }
                    }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    /**
     * 渲染性能指标对比柱状图
     * @param {Object} algorithmMode 算法模式最新数据
     * @param {Object} defaultMode 默认模式最新数据
     */
    function renderComparisonChart(algorithmMode, defaultMode) {
        const canvas = document.getElementById('comparison-chart');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        if (comparisonChartInstance) {
            comparisonChartInstance.destroy();
        }

        const algAvgResponseMin = (algorithmMode.avgResponseTimeMs / MS_TO_MINUTES) || 0;
        const noAlgAvgResponseMin = (defaultMode.avgResponseTimeMs / MS_TO_MINUTES) || 0;
        const algRevenue = algorithmMode.totalRevenue || 0;
        const noAlgRevenue = defaultMode.totalRevenue || 0;

        comparisonChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['平均响应时间 (分钟)', '总收益 (元)'],
                datasets: [
                    {
                        label: '优化算法',
                        // 注意：这里必须是数值类型，但toFixed()返回字符串，Chart.js可以处理
                        data: [algAvgResponseMin.toFixed(2), algRevenue.toFixed(2)],
                        backgroundColor: 'rgba(74, 88, 89, 0.8)', // 优化算法颜色 (深色)
                        borderColor: 'rgba(74, 88, 89, 1)',
                        borderWidth: 1
                    },
                    {
                        label: '默认模式',
                        data: [noAlgAvgResponseMin.toFixed(2), noAlgRevenue.toFixed(2)],
                        backgroundColor: 'rgba(179, 189, 190, 0.8)', // 默认模式颜色 (浅色)
                        borderColor: 'rgba(179, 189, 190, 1)',
                        borderWidth: 1
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, title: { display: true, text: '值' } },
                    x: { stacked: false }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                if (label) { label += ': '; }
                                // 根据数据索引添加单位
                                const unit = context.dataIndex === 0 ? '分钟' : '元';
                                label += context.formattedValue + ' ' + unit;
                                return label;
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 更新 UI 指标卡片
     * @param {Object} algorithmMode 算法模式最新数据
     * @param {Object} defaultMode 默认模式最新数据
     */
    function updateMetricsUI(algorithmMode, defaultMode) {
        if (!algorithmMode || !defaultMode) return;

        // 转换为分钟，并确保为数字（后端可能返回null或0）
        const algAvgResponseMin = (algorithmMode.avgResponseTimeMs / MS_TO_MINUTES) || 0;
        const noAlgAvgResponseMin = (defaultMode.avgResponseTimeMs / MS_TO_MINUTES) || 0;
        const algRevenue = algorithmMode.totalRevenue || 0;
        const noAlgRevenue = defaultMode.totalRevenue || 0;

        // 1. 更新指标卡片值
        $('#alg-avg-response').text(algAvgResponseMin.toFixed(2));
        $('#noalg-avg-response').text(noAlgAvgResponseMin.toFixed(2));
        $('#alg-revenue').text(algRevenue.toFixed(2));
        $('#noalg-revenue').text(noAlgRevenue.toFixed(2));

        // 2. 计算改进百分比
        let responseImprovement = 0;
        if (noAlgAvgResponseMin > 0) {
            // 响应时间越短越好，计算缩短的百分比
            responseImprovement = ((noAlgAvgResponseMin - algAvgResponseMin) / noAlgAvgResponseMin) * 100;
        }

        let revenueImprovement = 0;
        if (noAlgRevenue > 0) {
            // 收益越高越好，计算提升的百分比
            revenueImprovement = ((algRevenue - noAlgRevenue) / noAlgRevenue) * 100;
        }

        // 3. 更新改进百分比并设置颜色
        const updateImprovementText = (elementId, value) => {
            const element = $(`#${elementId}`);
            // 百分比显示一位小数
            const formattedValue = Math.abs(value).toFixed(1);

            // 判断是否为积极的改进 (响应时间缩短/收益提升)
            const isPositive = value > 0;

            element.text(`${formattedValue}%`);
            element.removeClass('improvement-positive improvement-negative');
            // 注意：HTML中已有improvement-positive类，这里保留并切换
            element.addClass(isPositive ? 'improvement-positive' : 'improvement-negative');
        };

        updateImprovementText('response-improvement', responseImprovement);
        updateImprovementText('revenue-improvement', revenueImprovement);
    }

    /**
     * 从后端 API 获取最新性能数据并渲染
     */
    async function fetchLatestMetrics() {
        try {
            // 可以在此处添加一个加载状态的UI提示

            const response = await fetch(API_URL);
            const data = await response.json();

            if (data.status === 'success') {
                const algMode = data.algorithmMode || {};
                const defMode = data.defaultMode || {};
                const algHistory = data.algorithmHistory || [];
                const defHistory = data.defaultHistory || [];

                // 1. 渲染最新的指标卡片
                updateMetricsUI(algMode, defMode);

                // 2. 渲染性能指标对比柱状图
                renderComparisonChart(algMode, defMode);

                // 3. 渲染历史收益折线图
                // 使用HTML中定义的颜色名称或预设的颜色
                const algColor = 'rgba(255, 202, 212, 0.8)'; // Pinkish/Red
                const defaultColor = 'rgba(199, 206, 234, 0.8)'; // Light Blue/Gray

                // 优化算法历史
                renderLineChart(
                    'alg-revenue-chart',
                    algHistory,
                    '优化算法总收益',
                    algColor
                );

                // 默认模式历史
                renderLineChart(
                    'noalg-revenue-chart',
                    defHistory,
                    '基础算法总收益',
                    defaultColor
                );

            } else {
                console.error("API Error:", data.message || "API返回错误状态。");
            }

        } catch (error) {
            console.error("Fetch Error:", error);
            // 可以在此处添加一个错误提示的UI
        }
    }

    // 页面加载完成后启动数据获取和定时刷新
    fetchLatestMetrics();
    // 设置定时器，每 10 秒刷新一次数据
    setInterval(fetchLatestMetrics, 8000);
});

// 初始化导航切换
function initNavigation() {
    $('#nav-dashboard').click(function(e) {
        e.preventDefault();
        showPage('dashboard-page');
        setActiveNav('nav-dashboard');
    });

    $('#nav-orders').click(function(e) {
        e.preventDefault();
        showPage('orders-page');
        setActiveNav('nav-orders');
    });

    $('#nav-robots').click(function(e) {
        e.preventDefault();
        showPage('robots-page');
        setActiveNav('nav-robots');
    });

    $('#nav-resources').click(function(e) {
        e.preventDefault();
        showPage('resources-page');
        setActiveNav('nav-resources');
    });

    // 订单筛选按钮
    $('#filter-orders').click(function() {
        renderOrders();
    });

    // 提交新订单 (调用修改后的函数)
    $('#submit-order').click(function() {
        createNewOrder();
        $('#new-order-modal').modal('hide');
    });

    // *新增：路径规划页面
    $('#nav-path-planning').click(function(e) {
        e.preventDefault();
        showPage('path-planning-page');
        setActiveNav('nav-path-planning');
        // 初始化路径规划页面
        initPathPlanning();
    });
}

// 显示指定页面
function showPage(pageId) {
    $('.page-content').addClass('d-none');
    $('#' + pageId).removeClass('d-none');
}

// 设置导航栏激活状态
function setActiveNav(navId) {
    $('.nav-link').removeClass('active');
    $('#' + navId).addClass('active');
}

// 初始化资源和地图 (替代原 initMockResourcesData, 仅保留地图初始化)
function initResourcesAndMap() {
    // 清空全局变量，等待后端数据
    orders = [];
    robots = [];
    utensils = [];
    workstations = [];

    // 初始化餐厅地图 (确保地图元素存在)
    initRestaurantMap();
}

// 模拟机器人位置更新 (由于 RobotVO 缺少 x, y 坐标，此逻辑仍需保留)
function updateRobotPositions() {
    robots.forEach(robot => {
        // 只有忙碌的机器人随机移动
        if (robot.status === 'busy') {
            robot.location.x = Math.max(50, Math.min(650, robot.location.x + (Math.random() - 0.5) * 30));
            robot.location.y = Math.max(50, Math.min(400, robot.location.y + (Math.random() - 0.5) * 30));
        }
    });

    // 更新地图上的机器人标记位置
    robots.forEach(robot => {
        $(`#robot-${robot.id}`).css({
            left: robot.location.x + 'px',
            top: robot.location.y + 'px'
        });
    });
}


// 创建新订单 (使用 AJAX POST 请求)
function createNewOrder() {
    // 获取选中的菜品 (假设前端的checkbox value是菜品ID)
    const dishIds = [];
    // 假设菜品复选框的class是 .dish-checkbox 且 value 是 dishId
    $('.dish-checkbox:checked').each(function() {
        dishIds.push(parseInt($(this).val()));
    });

    // 验证是否选择了菜品
    if (dishIds.length === 0) {
        showMemoryMessage('请至少选择一道菜品', false);
        return;
    }

    // 获取优先级和算法类型
    const priority = parseInt($('#order-priority').val());
    const algorithmType = $('#order-processing-algorithm').val();
    const notes = $('#order-notes').val(); // 备注信息

    // 发送创建订单请求到后端 (假设 OrderController 接受 /api/order 的 POST 请求)
    $.ajax({
        // FIX: 使用 API_BASE，路径改为 /api/order
        url: `${API_BASE}/orders`,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            // 假设后端DTO接收以下字段
            dishIds: dishIds,
            priority: priority,
            algorithmType: algorithmType,
            notes: notes
        }),
        success: function() {
            // 创建成功后重新获取订单列表
            fetchOrdersAndRender();
            // 重置表单
            $('#new-order-form')[0].reset();
            showMemoryMessage('订单创建成功！', true);
        },
        error: function(xhr, status, error) {
            console.error('创建订单失败:', error);
            showMemoryMessage(`创建订单失败，请重试。错误: ${xhr.status}`, false);
        }
    });
}


// ---------------------------------------------
// 【补充：数据渲染函数】
// ---------------------------------------------

// 渲染订单列表 (使用全局 orders 变量)
// 渲染订单列表 (使用全局 orders 变量)
function renderOrders() {
    const tableBody = $('#orders-table-body');
    const searchTerm = $('#order-search').val() ? $('#order-search').val().toLowerCase() : '';
    const statusFilter = $('#order-status-filter').val() || 'all'; // pending, processing, completed
    const priorityFilter = $('#order-priority-filter').val() || 'all';

    tableBody.empty(); // 清空旧数据

    // 筛选订单
    let filteredOrders = orders.filter(order => {
        // OrderVO 中是 orderId
        const matchesSearch = order.orderId && order.orderId.toString().toLowerCase().includes(searchTerm);
        // OrderVO 中是 status 字段（如 "pending"）
        const matchesStatus = statusFilter === 'all' || order.status === statusFilter;
        // OrderVO 中是 priority 字段
        const matchesPriority = priorityFilter === 'all' || order.priority && order.priority.toString() === priorityFilter;

        return matchesSearch && matchesStatus && matchesPriority;
    });

    // 按创建时间排序（最新的在前）
    filteredOrders.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));

    if (filteredOrders.length === 0) {
        tableBody.append(`<tr><td colspan="8" class="text-center text-muted py-4">暂无符合条件的订单</td></tr>`);
    }

    // 马卡龙色系状态样式映射
    const statusStyles = {
        'pending': {
            bgClass: 'bg-macaron-blue',
            textClass: 'text-macaron-blue-dark',
            label: '待处理'
        },
        'cooking': {
            bgClass: 'bg-macaron-yellow',
            textClass: 'text-macaron-yellow-dark',
            label: '烹饪中'
        },
        'completed': {
            bgClass: 'bg-macaron-pink',
            textClass: 'text-macaron-pink-dark',
            label: '已完成'
        },
    };

    // 渲染表格行
    filteredOrders.forEach(order => {
        // 获取当前状态的样式（默认使用待处理样式）
        const statusInfo = statusStyles[order.status] || statusStyles['pending'];

        // 优先级标签样式
        const priorityClass = order.priority >= 4
            ? 'bg-macaron-red text-macaron-red-dark'
            : order.priority >= 2
                ? 'bg-macaron-orange text-macaron-orange-dark'
                : 'bg-macaron-green text-macaron-green-dark';

        const row = `
            <tr class="${statusInfo.bgClass} transition-all duration-300 hover:shadow-sm">
                <td class="py-3 font-medium">${order.orderId || 'N/A'}</td>
                <td class="py-3">${order.dishName || 'N/A'}</td>
                <td class="py-3">
                    <span class="px-2 py-1 rounded-full text-sm ${priorityClass}">
                        ${order.priority || 'N/A'}
                    </span>
                </td>
                <td class="py-3 text-muted">${order.createTime || 'N/A'}</td>
                <td class="py-3">
                    <!-- 状态标签：拼接背景类和文字类 -->
                    <span class="px-3 py-1 rounded-full text-sm font-medium ${statusInfo.bgClass} ${statusInfo.textClass}">
                      ${statusInfo.label}
                    </span>
                </td>
                <td class="py-3">${order.robotId || '待分配'}</td>
                <td class="py-3 text-muted">${order.completeTime || '-'}</td>
                <td class="py-3">
                    <button class="btn btn-sm rounded-full bg-macaron-purple/20 text-macaron-purple-dark hover:bg-macaron-purple/30 transition-colors">
                        <i class="fas fa-info-circle me-1"></i>详情
                    </button>
                </td>
            </tr>
        `;
        tableBody.append(row);
    });
}

// 渲染仪表盘 (更新统计卡片和图表)
function renderDashboard() {
    // 统计数据
    const todayOrders = orders.length;
    // OrderVO 中的状态是字符串，如 "completed"
    const completedOrders = orders.filter(o => o.status === 'completed').length;
    const completionRate = todayOrders > 0 ? Math.round((completedOrders / todayOrders) * 100) : 0;
    // RobotVO 中的状态是前端转换后的 "idle"/"busy"
    const activeRobots = robots.filter(r => r.status === 'busy').length;
    const totalRobots = robots.length;

    // 计算平均出餐时间 (需要 OrderVO 有 createTime 和 completeTime)
    let avgTime = 0;
    const completed = orders.filter(o => o.status === 'completed' && o.completeTime);
    if (completed.length > 0) {
        completed.forEach(order => {
            const create = new Date(order.createTime);
            const complete = new Date(order.completeTime);
            avgTime += (complete - create) / (1000 * 60); // 转换为分钟
        });
        avgTime = Math.round((avgTime / completed.length) * 10) / 10;
    }

    // 更新统计卡片 (假设 HTML 中有对应的 ID)
    $('#today-orders').text(todayOrders);
    $('#completed-orders').text(completedOrders);
    $('#completion-rate').text(completionRate + '%');
    $('#active-robots').text(activeRobots);
    $('#total-robots').text(totalRobots);
    $('#avg-time').text(avgTime);

    // 渲染图表
    renderOrdersChart();
}


// 渲染机器人列表和地图上的标记 (使用全局 robots 变量)
function renderRobots() {
    const tableBody = $('#robots-table-body');
    const map = $('#restaurant-map');

    tableBody.empty(); // 清空旧数据
    map.find('.robot-marker').remove(); // 移除地图上的机器人标记

    robots.forEach(robot => {
        // 渲染表格行
        const statusClass = robot.status === 'idle' ? 'text-success' : 'text-danger';
        const row = `
            <tr>
                <td>${robot.id}</td>
                <td>${robot.name}</td>
                <td><span class="${statusClass} fw-bold">${robot.statusText}</span></td>
                <td>${robot.battery}%</td>
                <td>${robot.currentTask || '无'}</td>
                <td>${robot.completedOrders}</td>
            </tr>
        `;
        tableBody.append(row);

        // 渲染地图标记
        const markerClass = robot.status === 'idle' ? 'bg-success' : 'bg-danger';
        map.append(`<div class="robot-marker ${markerClass}" style="left: ${robot.location.x}px; top: ${robot.location.y}px;" id="robot-${robot.id}">${robot.id.substring(2)}</div>`);
    });
}

// 渲染资源列表 (器具和工作台)
function renderResources() {
    const toolTableBody = $('#utensils-table-body');
    const wsTableBody = $('#workstations-table-body');

    toolTableBody.empty();
    wsTableBody.empty();

    // 1. 渲染器具 (ToolVo)
    utensils.forEach(tool => {
        const statusClass = tool.status === 'available' ? 'text-success' : 'text-danger';
        const row = `
            <tr>
                <td>${tool.id}</td>
                <td>${tool.type}</td>
                <td><span class="${statusClass} fw-bold">${tool.statusText}</span></td>
                <td>${tool.robotId}</td>
            </tr>
        `;
        toolTableBody.append(row);
    });

    // 2. 渲染工作台 (WorkstationVo)
    workstations.forEach(ws => {
        const statusClass = ws.status === 'available' ? 'text-success' : 'text-danger';
        const row = `
            <tr>
                <td>${ws.id}</td>
                <td>${ws.capacity}KB</td>
                <td><span class="${statusClass} fw-bold">${ws.statusText}</span></td>
                <td>${ws.currentTask}</td>
                <td>${ws.robotId}</td>
            </tr>
        `;
        wsTableBody.append(row);
    });
}


// 初始化餐厅地图 (保留原文件的初始化逻辑)
function initRestaurantMap() {
    const map = $('#restaurant-map');

    // 清空旧的标记
    map.find('.station').remove();

    // 添加固定站点，位置适应更大的地图
    const stations = [
        { id: 'kitchen', name: '厨房', x: 100, y: 100 },
        { id: 'pickup1', name: '取餐点1', x: 350, y: 150 },
        { id: 'pickup2', name: '取餐点2', x: 500, y: 200 },
        { id: 'dining1', name: '用餐区1', x: 250, y: 300 },
        { id: 'dining2', name: '用餐区2', x: 450, y: 350 },
        { id: 'charging', name: '充电区', x: 150, y: 250 }
    ];
    stations.forEach(station => {
        map.append(`<div class="station" style="left: ${station.x}px; top: ${station.y}px;" id="station-${station.id}">${station.name}</div>`);
    });

    // 机器人标记会在 renderRobots 中添加
}

// 渲染订单趋势图表 (Chart.js 实现)
function renderOrdersChart() {
    const canvas = document.getElementById('orders-chart');
    // FIX: 增加 null 检查，防止找不到元素
    if (!canvas) return;

    const ctx = canvas.getContext('2d');

    // 按小时统计今天的订单
    const hours = Array(24).fill(0);
    const now = new Date();
    const today = now.toDateString();

    orders.forEach(order => {
        // 假设 OrderVO 的 createTime 是可解析的日期时间字符串
        const orderDate = new Date(order.createTime);
        if (orderDate.toDateString() === today) {
            const hour = orderDate.getHours();
            hours[hour]++;
        }
    });

    // 标签和数据
    const labels = [];
    const data = [];
    let hasData = false;
    hours.forEach((count, index) => {
        if (count > 0 || index === now.getHours()) {
            labels.push(index + ':00');
            data.push(count);
            if (count > 0) hasData = true;
        }
    });
    // 如果今天没有订单，仍然显示当前小时
    if (!hasData && labels.length === 0) {
        labels.push(now.getHours() + ':00');
        data.push(0);
    }


    // 销毁已存在的图表
    if (window.ordersChart) {
        window.ordersChart.destroy();
    }

    // 创建新图表
    window.ordersChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '订单数量',
                data: data,
                backgroundColor: 'rgba(168, 218, 220, 0.3)', // 马卡龙蓝
                borderColor: 'rgba(42, 123, 155, 1)',      // 深蓝色
                borderWidth: 2,
                tension: 0.3,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { precision: 0 }
                }
            }
        }
    });
}

// 渲染算法对比图表 (Chart.js 实现)
function renderAlgorithmComparisonChart() {
    const canvas = document.getElementById('comparison-chart');
    // FIX: 增加 null 检查，防止找不到元素
    if (!canvas) {
        console.error("HTML 元素 #comparison-chart 未找到，无法渲染图表。");
        return;
    }
    const ctx = canvas.getContext('2d');

    // 销毁已存在的图表
    if (window.comparisonChart) {
        window.comparisonChart.destroy();
    }

    const labels = ['平均响应时间 (分)', '吞吐量 (单/时)'];
    const dataAlg = [algorithmMetrics.withAlgorithm.avgResponseTime, algorithmMetrics.withAlgorithm.throughput];
    const dataNoAlg = [algorithmMetrics.withoutAlgorithm.avgResponseTime, algorithmMetrics.withoutAlgorithm.throughput];

    window.comparisonChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: '优化算法',
                    data: dataAlg,
                    backgroundColor: 'rgba(255, 202, 212, 0.8)', // 马卡龙粉
                    borderColor: 'rgb(214, 73, 51)',
                    borderWidth: 1
                },
                {
                    label: '基础算法',
                    data: dataNoAlg,
                    backgroundColor: 'rgba(199, 206, 234, 0.8)', // 马卡龙绿/紫
                    borderColor: 'rgb(80, 89, 140)',
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: 'y', // 横向柱状图
            scales: {
                x: {
                    beginAtZero: true
                }
            }
        }
    });
}
// 路径规划相关变量
let requestPositions = [];
let warehouseData = {};
let lastSchedule = null;
let animation = {
    points: [],
    progressIndex: 0,
    min: 0,
    max: 0,
    interval: null,
    speed: 1
};

// 路径规划API接口配置
const pathPlanningApi = {
    warehouse: `${API_BASE}/warehouse`,
    random: `${API_BASE}/requests/random`,
    schedule: `${API_BASE}/schedule`
};

// 初始化路径规划页面
function initPathPlanning() {
    // 加载仓库数据
    loadWarehouse();

    // 绑定按钮事件
    $('#btn-generate').click(generateRequests);
    $('#btn-run').click(runSchedule);
    $('#btn-play').click(playAnimation);
    $('#btn-pause').click(pauseAnimation);
    $('#btn-step').click(stepAnimation);
    $('#btn-reset').click(resetAnimation);
    $('#btn-compare').click(runCompare);
    $('#speed').change(function() {
        animation.speed = parseFloat($(this).val());
        if (animation.interval) {
            pauseAnimation();
            playAnimation();
        }
    });
}

// 工具函数：获取DOM元素
function el(id) {
    return document.getElementById(id);
}

// 清除路径显示
function clearLane(lane) {
    while (lane.firstChild) {
        if (!lane.firstChild.classList.contains('tick') && !lane.firstChild.classList.contains('tick-label')) {
            lane.removeChild(lane.firstChild);
        } else {
            lane.removeChild(lane.firstChild);
        }
    }
}

// 计算边界
function laneBounds(positions) {
    if (positions.length === 0) return { min: 0, max: 10 };
    return {
        min: Math.min(...positions),
        max: Math.max(...positions)
    };
}

// 绘制刻度
function drawTicks(lane, min, max) {
    const width = lane.clientWidth - 16;
    const ticks = 10;
    const step = (max - min) / ticks;

    for (let i = 0; i <= ticks; i++) {
        const value = min + (step * i);
        const x = 8 + (i / ticks) * width;

        const tick = document.createElement('div');
        tick.className = 'tick';
        tick.style.left = `${x}px`;
        lane.appendChild(tick);

        const label = document.createElement('div');
        label.className = 'tick-label';
        label.style.left = `${x}px`;
        label.textContent = Math.round(value);
        lane.appendChild(label);
    }
}

// 绘制点
function drawPoints(lane, positions, min, max, color) {
    const width = lane.clientWidth - 16;
    positions.forEach(pos => {
        const ratio = (pos - min) / (max - min || 1);
        const x = 8 + (ratio * width);

        const point = document.createElement('div');
        point.className = 'point';
        point.style.left = `${x}px`;
        point.style.backgroundColor = color;
        lane.appendChild(point);

        const label = document.createElement('div');
        label.className = 'point label';
        label.style.left = `${x}px`;
        label.textContent = pos;
        lane.appendChild(label);
    });
}

// 加载仓库数据
async function loadWarehouse() {
    try {
        const res = await fetch(pathPlanningApi.warehouse);
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        warehouseData = await res.json();
        const positions = Object.keys(warehouseData).map(Number);
        const lane = el('warehouse');
        clearLane(lane);
        const { min, max } = laneBounds(positions);
        drawTicks(lane, min, max);
        drawPoints(lane, positions, min, max, '#888');
    } catch (error) {
        console.error("加载仓库数据失败:", error);
    }
}

// 生成随机请求
async function generateRequests() {
    try {
        const count = Number(el('count').value || 6);
        const res = await fetch(`${pathPlanningApi.random}?count=${count}`);
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        requestPositions = await res.json();
        const lane = el('requests');
        clearLane(lane);
        const { min, max } = laneBounds(requestPositions);
        drawTicks(lane, min, max);
        drawPoints(lane, requestPositions, min, max, '#888');
    } catch (error) {
        console.error("生成请求失败:", error);
    }
}

// 运行调度
async function runSchedule() {
    try {
        const algorithm = el('algorithm').value;
        el('alg-label').textContent = algorithm;
        const initial = Number(el('initial').value || 0);

        const res = await fetch(`${pathPlanningApi.schedule}?algorithm=${algorithm}&initialPosition=${initial}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestPositions)
        });

        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        const data = await res.json();

        const lane = el('path');
        clearLane(lane);
        const positions = [...requestPositions, initial];
        const { min, max } = laneBounds(positions);
        drawTicks(lane, min, max);
        drawPoints(lane, requestPositions, min, max, '#888');

        // 保存并展示关键信息
        lastSchedule = { data, initial, min, max };
        updateScheduleInfo(lastSchedule);
    } catch (error) {
        console.error("运行调度失败:", error);
    }
}

// 更新调度信息显示
function updateScheduleInfo(schedule) {
    const { data, initial, min, max } = schedule;
    const endPos = (data.processedOrder && data.processedOrder.length)
        ? data.processedOrder[data.processedOrder.length - 1]
        : initial;

    el('info-start').textContent = String(initial);
    el('info-end').textContent = String(endPos);
    el('info-distance').textContent = String(data.totalDistance);

    // 估算时间
    const lane = el('path');
    const basePixelsPerSec = 120, widthPx = lane.clientWidth;
    const spanPx = Math.max(1, widthPx - 16);
    const domain = Math.max(1, max - min);
    const pixelsPerUnit = spanPx / domain;
    const px = (data.totalDistance || 0) * pixelsPerUnit;
    const seconds = px / basePixelsPerSec;
    el('info-time').textContent = `${seconds.toFixed(2)} s (1x)`;

    // 显示结果详情
    el('result').textContent = [
        `算法: ${data.algorithmName}`,
        `处理顺序: ${JSON.stringify(data.processedOrder)}`,
        `每步距离: ${JSON.stringify(data.stepDistances)}`,
        `总距离: ${data.totalDistance}`,
        ...(data.stepDetails || [])
    ].join('\n');
}

// 动画控制函数
function playAnimation() {
    if (!lastSchedule || animation.interval) return;

    animation = {
        ...lastSchedule,
        progressIndex: 0,
        interval: setInterval(() => {
            stepAnimation();
        }, 1000 / animation.speed)
    };
}

function pauseAnimation() {
    if (animation.interval) {
        clearInterval(animation.interval);
        animation.interval = null;
    }
}

function stepAnimation() {
    if (!lastSchedule) return;

    const lane = el('path');
    const head = document.getElementById('anim-head') || document.createElement('div');
    if (!document.getElementById('anim-head')) {
        head.id = 'anim-head';
        head.className = 'head';
        lane.appendChild(head);
    }

    const { data, min, max } = lastSchedule;
    if (!data.processedOrder || animation.progressIndex >= data.processedOrder.length - 1) {
        pauseAnimation();
        return;
    }

    const width = lane.clientWidth;
    const from = animation.progressIndex === 0 ? lastSchedule.initial : data.processedOrder[animation.progressIndex - 1];
    const to = data.processedOrder[animation.progressIndex];

    const xFrom = 8 + ((from - min) / (max - min || 1)) * (width - 16);
    const xTo = 8 + ((to - min) / (max - min || 1)) * (width - 16);

    head.style.left = `${xTo}px`;

    const seg = document.createElement('div');
    seg.className = 'seg';
    seg.style.left = `${Math.min(xFrom, xTo)}px`;
    seg.style.width = `${Math.abs(xTo - xFrom)}px`;
    lane.appendChild(seg);

    animation.progressIndex++;
}

function resetAnimation() {
    pauseAnimation();
    const lane = el('path');
    const head = document.getElementById('anim-head');
    if (head) lane.removeChild(head);
    document.querySelectorAll('.seg').forEach(seg => seg.remove());
    animation.progressIndex = 0;
}

// 算法对比
async function runCompare() {
    try {
        const initial = Number(el('initial').value || 0);
        const body = el('comparison-results');
        body.innerHTML = '';
        const algos = ['FCFS', 'SSTF', 'SCAN'];

        const requests = requestPositions;
        if (requests.length === 0) {
            alert('请先生成请求位置');
            return;
        }

        // 并行请求三种算法结果
        const calls = algos.map(algo =>
            fetch(`${pathPlanningApi.schedule}?algorithm=${algo}&initialPosition=${initial}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requests)
            }).then(r => r.json().then(data => ({ algo, data })))
        );

        const results = await Promise.all(calls);
        const positions = [...requests, initial];
        const { min, max } = laneBounds(positions);
        const lane = el('path');
        const spanPx = Math.max(1, lane.clientWidth - 16);
        const domain = Math.max(1, max - min);
        const pixelsPerUnit = spanPx / domain;
        const basePixelsPerSec = 120;

        // 显示对比结果
        results.forEach(({ algo, data }) => {
            const endPos = (data.processedOrder && data.processedOrder.length)
                ? data.processedOrder[data.processedOrder.length - 1]
                : initial;
            const px = (data.totalDistance || 0) * pixelsPerUnit;
            const sec = px / basePixelsPerSec;

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${algo}</td>
                <td>${JSON.stringify(data.processedOrder)}</td>
                <td>${data.totalDistance}</td>
                <td>${endPos}</td>
                <td>${sec.toFixed(2)}s</td>
            `;
            body.appendChild(tr);
        });
    } catch (error) {
        console.error("算法对比失败:", error);
    }
}