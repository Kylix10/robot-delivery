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

//仓库食材拿取的新增变量
// 调度可视化常量
const VERTICAL_LAYER_HEIGHT = 25;
const MIN_HORIZONTAL_SEPARATION_PERCENT = 6;
const WAREHOUSE_MAX_CAPACITY = 100;

// 调度可视化状态
let drawnPointsRecord = {}; // 格式: {laneId: [{pos: 15, layer: 0}, ...]}
let orderScheduleResult = null;
let currentAlgorithm = 'FCFS';

// 动画控制变量
let currentStep = 0;
let isPlaying = false;
let animationInterval = null;
let animationSpeed = 1000;

// 页面元素ID映射
const ELEMENT_IDS = {
    WAREHOUSE_LANE: 'warehouse',
    REQUEST_LANE: 'requests',
    PATH_LANE: 'path',
    RESULT_PANEL: 'result',
    ALGORITHM_LABEL: 'alg-label',
    INIT_POS: 'info-start',
    END_POS: 'info-end',
    TOTAL_DISTANCE: 'info-distance',

    ALGORITHM_SELECT: 'algorithm-select',
    DISH_NAME: 'dish-name-display',

    ORDER_ID_INPUT: 'order-id',
    LOAD_BUTTON: 'btn-load-order',
    COMPARE_BUTTON: 'btn-compare', // 算法对比按钮 ID
    MESSAGE_AREA: 'message-area',

    PLAY_BUTTON: 'btn-play',
    PAUSE_BUTTON: 'btn-pause',
    STEP_BUTTON: 'btn-step',
    RESET_BUTTON: 'btn-reset',
    SPEED_SELECT: 'speed'
};
// -----------------------------------------------------------

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

// 从后端获取算法指标数据 (保留模拟逻辑，因为未提供后端API)
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

// 模拟算法指标数据 (将吞吐量改为收益)
function simulateAlgorithmMetrics() {
    // 模拟使用优化算法的情况 - 更好的性能和收益
    algorithmMetrics.withAlgorithm.avgResponseTime = (3 + Math.random() * 2).toFixed(1); // 3-5分钟
    algorithmMetrics.withAlgorithm.revenue = (1200 + Math.random() * 400).toFixed(2);     // 1200-1600元/小时

    // 模拟不使用算法的情况 - 较差的性能和收益
    algorithmMetrics.withoutAlgorithm.avgResponseTime = (6 + Math.random() * 3).toFixed(1); // 6-9分钟
    algorithmMetrics.withoutAlgorithm.revenue = (800 + Math.random() * 300).toFixed(2);      // 800-1100元/小时
}
// 新增：更新历史收益数据并渲染折线图
function updateRevenueHistory(metrics, type) {
    const revenue = parseFloat(metrics.revenue);
    const newEntry = {
        time: metrics.revenueHistory.length + 1, // 记录是第几分钟
        value: revenue // 当前收益值
    };

    // 限制历史数据点数量
    if (metrics.revenueHistory.length >= 20) {
        metrics.revenueHistory.shift(); // 移除最旧的点
    }
    metrics.revenueHistory.push(newEntry);

    // 仅在历史数据更新时渲染折线图
    if (type === 'withAlgorithm') {
        renderRevenueChart('alg-revenue-chart', algorithmMetrics.withAlgorithm.revenueHistory, '使用优化算法总收益 (元)', 'rgba(255, 202, 212, 0.8)');
    } else {
        renderRevenueChart('noalg-revenue-chart', algorithmMetrics.withoutAlgorithm.revenueHistory, '使用基础算法总收益 (元)', 'rgba(199, 206, 234, 0.8)');
    }
}
// 更新算法指标UI
// 修改updateAlgorithmMetricsUI
function updateAlgorithmMetricsUI() {
    // 更新指标卡片
    $('#alg-avg-response').text(algorithmMetrics.withAlgorithm.avgResponseTime);
    $('#noalg-avg-response').text(algorithmMetrics.withoutAlgorithm.avgResponseTime);

    // 更新收益卡片
    $('#alg-revenue').text(algorithmMetrics.withAlgorithm.revenue);
    $('#noalg-revenue').text(algorithmMetrics.withoutAlgorithm.revenue);

    // 计算改进百分比
    const responseImprovement = Math.round((1 -
        algorithmMetrics.withAlgorithm.avgResponseTime / algorithmMetrics.withoutAlgorithm.avgResponseTime) * 100);
    const revenueImprovement = Math.round((
        algorithmMetrics.withAlgorithm.revenue / algorithmMetrics.withoutAlgorithm.revenue - 1) * 100);

    // 更新改进百分比并设置颜色
    $('#response-improvement').text(responseImprovement + '%').removeClass('improvement-positive improvement-negative')
        .addClass(responseImprovement > 0 ? 'improvement-positive' : 'improvement-negative');
    $('#revenue-improvement').text(revenueImprovement + '%').removeClass('improvement-positive improvement-negative')
        .addClass(revenueImprovement > 0 ? 'improvement-positive' : 'improvement-negative');

    // 渲染对比图表 (如果需要，可保留此柱状图)
    //renderAlgorithmComparisonChart();
}

// 新增：渲染收益折线图
function renderRevenueChart(chartId, dataArray, title, color) {
    const canvas = document.getElementById(chartId);
    if (!canvas) {
        console.error(`HTML 元素 #${chartId} 未找到，无法渲染图表。`);
        return;
    }
    const ctx = canvas.getContext('2d');

    // 销毁已存在的图表
    if (window[chartId + 'Chart']) {
        window[chartId + 'Chart'].destroy();
    }

    const labels = dataArray.map(d => `${d.time} min`);
    const data = dataArray.map(d => d.value);

    window[chartId + 'Chart'] = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: title,
                data: data,
                backgroundColor: color,
                borderColor: color.replace('0.8', '1'),
                borderWidth: 2,
                tension: 0.3,
                fill: false, // 折线图不填充
                pointRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: '收益 (元)'
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: '时间 (分钟)'
                    }
                }
            }
        }
    });
}

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


// ===================================================================
// 2. 辅助函数 (el, DOM 操作)
// ===================================================================

function el(id) {
    return document.getElementById(id);
}

function showMessage(text, isSuccess) {
    const msgArea = $(`#${ELEMENT_IDS.MESSAGE_AREA}`);
    if (!msgArea.length) return;
    const className = isSuccess ? 'alert-success' : 'alert-danger';
    msgArea.html(`<div class="alert ${className}" role="alert">${text}</div>`);
    setTimeout(() => { msgArea.empty(); }, 4000);
}

// ------------------- 可视化辅助函数 -------------------

function clearLane(laneId) {
    const lane = el(laneId);
    if (lane) {
        // 🔹 优化容器尺寸与风格（更小、更干净）
        lane.innerHTML = `
<div class="lane-title font-semibold text-gray-700 mb-1">
    ${laneId === ELEMENT_IDS.WAREHOUSE_LANE ? '仓库分布 (0-100)' : laneId === ELEMENT_IDS.REQUEST_LANE ? '请求位置' : '调度路径'}
</div>
<div class="lane-visualization" style="
    position: relative;
    height: 90px;
    background-color: #f1f3f5;
    border-radius: 6px;
    margin-top: 4px;
    box-shadow: inset 0 1px 2px rgba(0,0,0,0.08);
    overflow: hidden;">
    <div style="
        position: absolute;
        top: 50%;
        left: 0;
        right: 0;
        height: 1.5px;
        background-color: #adb5bd;
        z-index: 5;"></div>
</div>`;
    }
}

function drawTicks(lane, min, max) {
    const vizContainer = $(lane).find('.lane-visualization');
    if (vizContainer.length === 0) {
        clearLane($(lane).attr('id'));
    }
    // (刻度线逻辑略)
}

const VISUAL_PADDING_PERCENT = 3; // 左右边距更紧凑

function positionToPercent(pos) {
    const clampedPos = Math.max(0, Math.min(pos, WAREHOUSE_MAX_CAPACITY));
    const visualRange = 100 - 2 * VISUAL_PADDING_PERCENT;
    const scaledPos = (clampedPos / WAREHOUSE_MAX_CAPACITY) * visualRange;
    const finalPercent = VISUAL_PADDING_PERCENT + scaledPos;
    return finalPercent + '%';
}

/**
 * 绘制位置点，自动错开重叠
 */
function drawPositionPoint(lane, pos, color, label, showLabelBelow = false, overlapIndex = 0) {
    const vizContainer = $(lane).find('.lane-visualization');
    if (vizContainer.length === 0) return;

    // 🔹 优化垂直偏移逻辑，自动交错，间距更紧凑
    const VERTICAL_OFFSET_INCREMENT = 24;
    const isUp = overlapIndex % 2 === 0;
    const step = Math.floor(overlapIndex / 2);
    const totalOffset = isUp ? -step * VERTICAL_OFFSET_INCREMENT : step * VERTICAL_OFFSET_INCREMENT;
    const pointTopPos = `calc(50% + ${totalOffset}px)`;

    // 绘制点
    const pointDiv = document.createElement('div');
    pointDiv.className = 'point drawn-point';
    pointDiv.title = label;
    pointDiv.textContent = pos;

    $(pointDiv).css({
        position: 'absolute',
        left: positionToPercent(pos),
        top: pointTopPos,
        transform: 'translate(-50%, -50%)',
        padding: '4px 8px',
        fontSize: '12px',
        fontWeight: '600',
        borderRadius: '10px',
        backgroundColor: color,
        color: 'white',
        zIndex: '20',
        minWidth: '30px',
        textAlign: 'center',
        boxShadow: `0 2px 4px rgba(0,0,0,0.15), 0 0 0 1.5px ${color}`
    });

    vizContainer.append(pointDiv);

    // 绘制标签（仓库使用）
    if (showLabelBelow) {
        const labelDiv = document.createElement('div');
        labelDiv.className = 'point-label';
        labelDiv.textContent = label;
        const LABEL_OFFSET_FROM_POINT_CENTER = 22;
        const labelTopPos = `calc(${pointTopPos} + ${LABEL_OFFSET_FROM_POINT_CENTER}px)`;

        $(labelDiv).css({
            position: 'absolute',
            left: positionToPercent(pos),
            top: labelTopPos,
            transform: 'translateX(-50%)',
            fontSize: '9px',
            color: '#6c757d',
            whiteSpace: 'nowrap',
            zIndex: '15'
        });

        vizContainer.append(labelDiv);
    }
}

/**
 * 绘制路径线段 (动画)
 */
function drawPathSegments(lane, initialPos, processedOrder, currentStep) {
    const vizContainer = $(lane).find('.lane-visualization');
    if (vizContainer.length === 0) return;

    const fullPath = [initialPos, ...processedOrder];
    const PATH_LINE_Y_POS = '50%';
    const PATH_LINE_HEIGHT = '3px';

    // 路径线 - 使用马卡龙蓝
    for (let i = 1; i <= currentStep && i < fullPath.length; i++) {
        const startPos = fullPath[i - 1];
        const endPos = fullPath[i];
        const startPercent = parseFloat(positionToPercent(Math.min(startPos, endPos)));
        const endPercent = parseFloat(positionToPercent(Math.max(startPos, endPos)));
        const lengthPercent = endPercent - startPercent;

        const segmentDiv = document.createElement('div');
        segmentDiv.className = 'path-segment-drawn';
        $(segmentDiv).css({
            position: 'absolute',
            left: startPercent + '%',
            width: lengthPercent + '%',
            top: `calc(${PATH_LINE_Y_POS} - ${parseInt(PATH_LINE_HEIGHT) / 2}px)`,
            height: PATH_LINE_HEIGHT,
            backgroundColor: 'var(--macaron-blue)', // 马卡龙蓝
            borderRadius: '2px',
            boxShadow: '0 0 4px rgba(168, 218, 220, 0.8)',
            zIndex: '10',
            opacity: '1'
        });
        vizContainer.append(segmentDiv);
    }

    // 标记已完成点 - 使用马卡龙绿深色
    for (let i = 1; i <= currentStep && i < fullPath.length; i++) {
        const completedPos = fullPath[i];
        const endPointDiv = document.createElement('div');
        endPointDiv.className = 'point drawn-point-path';
        endPointDiv.title = `已拿取 ${completedPos}`;
        endPointDiv.textContent = completedPos;

        $(endPointDiv).css({
            position: 'absolute',
            left: positionToPercent(completedPos),
            transform: 'translate(-50%, -50%)',
            top: PATH_LINE_Y_POS,
            padding: '8px',
            fontSize: '13px',
            fontWeight: 'bold',
            borderRadius: '50%',
            backgroundColor: 'var(--macaron-green-dark)', // 马卡龙绿深色
            color: 'white',
            zIndex: '30',
            minWidth: '34px',
            textAlign: 'center',
            boxShadow: '0 0 8px rgba(79, 101, 80, 0.7), inset 0 0 4px white'
        });
        vizContainer.append(endPointDiv);
    }
}


// ===================================================================
// 3. 动画和调度控制逻辑
// ===================================================================

/**
 * 获取当前选择的算法结果
 */
function getCurrentSchedulerResult() {
    if (!orderScheduleResult || !orderScheduleResult.algorithmResults) return null;
    return orderScheduleResult.algorithmResults[currentAlgorithm];
}

/**
 * 渲染路径的特定步骤
 */
function renderPathStep(currentResult, step) {
    const lane = el(ELEMENT_IDS.PATH_LANE);
    if (!lane || !currentResult.processedOrder) return;

// 清空并重建容器
    clearLane(ELEMENT_IDS.PATH_LANE);
    const updatedVizContainer = $(el(ELEMENT_IDS.PATH_LANE)).find('.lane-visualization');

    const processedOrder = currentResult.processedOrder;
// 仓库机械臂的初始位置固定为 0
    const initialPos = 0;

    drawTicks(lane, 0, WAREHOUSE_MAX_CAPACITY);

// 1. 绘制路径线段和已完成的点
    drawPathSegments(lane, initialPos, processedOrder, step);

// 2. 确定当前机械臂位置
    const currentPath = [initialPos, ...processedOrder];
    const currentRobotPos = currentPath[step] || initialPos; // 确保在 step 0 时是 initialPos

// 3. 绘制机械臂当前位置 (马卡龙粉深色)
    const robotPointDiv = document.createElement('div');
    robotPointDiv.className = 'point drawn-robot';
    robotPointDiv.title = '机械臂当前位置';
    robotPointDiv.textContent = currentRobotPos;

    // 机械臂头部样式优化：更突出，使用马卡龙粉深色
    $(robotPointDiv).css({
        position: 'absolute',
        left: positionToPercent(currentRobotPos),
        transform: 'translateX(-50%)',
        top: '10px', // 保持在上方，避开中心线和路径
        padding: '8px 16px',
        fontSize: '20px',
        fontWeight: '700',
        borderRadius: '50% / 10%', // 特殊的圆角/椭圆形状
        backgroundColor: 'var(--macaron-pink-dark)', // 马卡龙粉深色
        color: 'white',
        zIndex: '60', // 最高的 Z-Index
        // 增加马卡龙粉色光晕阴影
        boxShadow: '0 0 15px rgba(214, 73, 51, 0.8), 0 4px 8px rgba(0,0,0,0.4)',
        // 启用过渡，移动时更平滑
        transition: 'left 0.5s ease-out',
    });
    updatedVizContainer.append(robotPointDiv);

// 4. 更新调度信息
    const totalDistanceCovered = currentResult.stepDistances
        .slice(0, step)
        .reduce((sum, dist) => sum + dist, 0);

// 确保 stepDetails 在 step 0 时不越界
    const latestAction = (step > 0 && currentResult.stepDetails && currentResult.stepDetails[step - 1])
        ? currentResult.stepDetails[step - 1]
        : '等待指令';

    el(ELEMENT_IDS.ALGORITHM_LABEL).textContent = currentResult.algorithmName;
    el(ELEMENT_IDS.INIT_POS).textContent = initialPos;
    el(ELEMENT_IDS.END_POS).textContent = currentRobotPos;
    el(ELEMENT_IDS.TOTAL_DISTANCE).textContent = totalDistanceCovered;

// 5. 更新结果面板 (显示动态路径)
    const currentOrderSequence = currentPath.slice(0, step + 1);
    const resultPanel = el(ELEMENT_IDS.RESULT_PANEL);
    if (resultPanel) {
        resultPanel.innerHTML = `
<p><strong>当前路径:</strong> ${currentOrderSequence.join(' → ')}</p>
<p><strong>当前总距离:</strong> ${totalDistanceCovered}</p>
<p><strong>最新动作:</strong> ${latestAction}</p>
`;
    }
}

/**
 * 动画控制：播放/继续
 */
function playAnimation() {
    if (isPlaying || !orderScheduleResult) return;
    isPlaying = true;

    const result = getCurrentSchedulerResult();
    if (!result) {
        showMessage('请选择一个有效的算法或加载订单数据！', false);
        isPlaying = false;
        return;
    }

    const maxSteps = result.processedOrder.length;

    if (currentStep >= maxSteps) {
        currentStep = 0; // 重头开始
    }

// 禁用 Play 按钮，启用 Pause 按钮
    el(ELEMENT_IDS.PLAY_BUTTON).disabled = true;
    el(ELEMENT_IDS.PAUSE_BUTTON).disabled = false;

    animationInterval = setInterval(() => {
        if (currentStep < maxSteps) {
            currentStep++;
            renderPathStep(result, currentStep);
        } else {
            pauseAnimation();
            renderSchedulerResultStatic(result); // 播放完毕显示最终结果
        }
    }, animationSpeed);
}

/**
 * 动画控制：暂停
 */
function pauseAnimation() {
    if (animationInterval) {
        clearInterval(animationInterval);
        animationInterval = null;
    }
    isPlaying = false;
// 启用 Play 按钮，禁用 Pause 按钮
    el(ELEMENT_IDS.PLAY_BUTTON).disabled = false;
    el(ELEMENT_IDS.PAUSE_BUTTON).disabled = true;
}

/**
 * 动画控制：单步执行
 */
function stepAnimation() {
    pauseAnimation(); // 确保暂停
    const result = getCurrentSchedulerResult();
    if (!result || !result.processedOrder) return;

    const maxSteps = result.processedOrder.length;
    if (currentStep < maxSteps) {
        currentStep++;
        renderPathStep(result, currentStep);
    } else {
        renderSchedulerResultStatic(result);
    }
}

/**
 * 动画控制：重置
 */
function resetAnimation() {
    pauseAnimation();
    currentStep = 0;
    const result = getCurrentSchedulerResult();
    if (result) {
        renderPathStep(result, currentStep); // 渲染到初始位置 0
        renderSchedulerResultStatic(result); // 显示最终结果（总距离、顺序等）
    }
}

/**
 * 渲染最终的静态结果（通常用于动画结束或重置后）
 */
function renderSchedulerResultStatic(result) {
    if (!result) return;

    const resultPanel = el(ELEMENT_IDS.RESULT_PANEL);
    if (resultPanel) {
// 总移动距离，显示最终结果的距离
        const finalDistance = result.totalDistance;

// 步骤详情列表
        const detailsList = result.stepDetails && result.stepDetails.length > 0
            ? `<ul class="list-disc list-inside space-y-1 mt-2">${result.stepDetails.map(d => `<li class="text-sm text-gray-700">${d}</li>`).join('')}</ul>`
            : '<p class="text-sm text-gray-500">暂无详细步骤。</p>';

        resultPanel.innerHTML = `
<p><strong>最终处理顺序:</strong> <span class="text-indigo-600 font-mono">${[0, ...result.processedOrder].join(' → ')}</span></p>
<p><strong>总移动距离:</strong> <span class="text-green-600 font-bold">${finalDistance}</span></p>
<h4 class="font-semibold mt-3 mb-1 text-base border-b pb-1">算法详情</h4>
${detailsList}
`;
    }
}

/**
 * 绘制静态区域（仓库分布和请求队列）
 */
function drawStaticVisualizations(scheduleResult) {
    const warehouseLane = el(ELEMENT_IDS.WAREHOUSE_LANE);
    const requestLane = el(ELEMENT_IDS.REQUEST_LANE);

// 默认使用 FCFS 的结果来获取仓库和请求点数据
    const result = scheduleResult.algorithmResults['FCFS'];

    clearLane(ELEMENT_IDS.WAREHOUSE_LANE);
    clearLane(ELEMENT_IDS.REQUEST_LANE);

    drawTicks(warehouseLane, 0, WAREHOUSE_MAX_CAPACITY);
    drawTicks(requestLane, 0, WAREHOUSE_MAX_CAPACITY);

    const warehouseOverlapMap = {}; // 存储 {position: count}
    const requestOverlapMap = {}; // 存储 {position: count}

// 绘制仓库所有食材点 (马卡龙绿)
    Object.entries(result.warehouseIngredients).forEach(([posStr, label]) => {
        const pos = parseInt(posStr);
        const overlapCount = warehouseOverlapMap[pos] || 0;

// 绘制点，使用马卡龙绿
        drawPositionPoint(warehouseLane, pos, 'var(--macaron-green)', label, true, overlapCount);

// 更新重叠计数器
        warehouseOverlapMap[pos] = overlapCount + 1;
    });

// 绘制原始请求队列点 (马卡龙橙)
    result.requestedPositions.forEach(pos => { // 使用 requestedPositions 来获取原始请求队列
        const overlapCount = requestOverlapMap[pos] || 0;

// 绘制点，使用马卡龙橙
        drawPositionPoint(requestLane, pos, 'var(--macaron-orange)', `请求: ${pos}`, false, overlapCount);

// 更新重叠计数器
        requestOverlapMap[pos] = overlapCount + 1;
    });

    if (scheduleResult.dishName && el(ELEMENT_IDS.DISH_NAME)) {
        el(ELEMENT_IDS.DISH_NAME).textContent = scheduleResult.dishName;
    }
}

/**
 * 核心：加载和处理订单数据
 */
function loadOrderData(orderId) {
    const API_URL = `${API_BASE}/order-scheduler/${orderId}`;
    showMessage(`正在加载订单 ${orderId} 并运行调度算法...`, false);
    $.ajax({
        url: API_URL,
        method: 'GET',
        success: function(response) {
            orderScheduleResult = response;
            // ==========================================================
            // 【新增/修改】：确保算法选择框被填充和 currentAlgorithm 初始化
            // ==========================================================
            const algSelect = el(ELEMENT_IDS.ALGORITHM_SELECT);
            if (algSelect) {
                $(algSelect).empty(); // 清空旧选项
                const algorithms = Object.keys(orderScheduleResult.algorithmResults);

                algorithms.forEach(algKey => {
                    const result = orderScheduleResult.algorithmResults[algKey];
                    // 假设 Option 的 value 是算法的 key，text 是算法的 Name
                    $(algSelect).append(new Option(result.algorithmName, algKey));
                });

                // 设置默认选中的算法，确保 currentAlgorithm 有值
                // 默认选择 FCFS，如果不存在则选择列表第一个
                currentAlgorithm = algorithms.includes('FCFS') ? 'FCFS' : algorithms[0];
                $(algSelect).val(currentAlgorithm); // 更新选择框的值
            } else {
                currentAlgorithm = 'FCFS'; // 如果没有选择框，默认使用 FCFS
            }
            // 【FIX 1: 确保原始请求队列数据存在】
            const fcfsResult = orderScheduleResult.algorithmResults['FCFS'];
            if (fcfsResult) {
                if (!fcfsResult.requestedPositions && fcfsResult.processedOrder) {
                    // 临时兼容：如果后端未提供 requestedPositions，则使用 processedOrder 中的点
                    orderScheduleResult.algorithmResults['FCFS'].requestedPositions = [...fcfsResult.processedOrder];
                }
                if (!fcfsResult.requestedPositions) {
                    // 确保它至少是一个空数组，防止 forEach 报错
                    orderScheduleResult.algorithmResults['FCFS'].requestedPositions = [];
                }
            }

            drawStaticVisualizations(orderScheduleResult);
            resetAnimation();

            // 【FIX 2: 确保算法对比表格首次渲染】
            // *重要：在数据加载成功后，必须调用一次渲染函数来填充对比表格。
            renderAlgorithmComparison();

            // ... (其他原有逻辑)
            drawStaticVisualizations(orderScheduleResult);
            resetAnimation();


            showMessage(`订单 ${orderId} 调度成功！`, true);
        },

        error: function(xhr, status, error) {

// 如果返回的是 4xx/5xx 错误，且 body 是字符串，则显示 body

            let errorText = error;

            if (xhr.responseText) {

// 尝试解析后端返回的错误信息（可能是纯文本或JSON）

                try {

                    const jsonResponse = JSON.parse(xhr.responseText);

// 假设后端返回的错误信息在 reason 或 message 字段

                    errorText = jsonResponse.reason || jsonResponse.message || xhr.responseText;

                } catch (e) {

                    errorText = xhr.responseText; // 如果不是JSON，就用原始文本

                }

            } else if (xhr.status === 404) {

                errorText = '找不到订单调度服务接口 (404)。请检查后端路径是否为 /api/order-scheduler/{id}';

            }

            showMessage(`加载订单失败 (${xhr.status}): ${errorText}`, false);

            orderScheduleResult = null;

        }

    });

}

function renderAlgorithmComparison() {

// 检查 orderScheduleResult 是否已加载

    if (!orderScheduleResult || !orderScheduleResult.algorithmResults) return;

    const tableBody = $('#comparison-results');
    tableBody.empty(); // 清空原有内容

    const algorithms = orderScheduleResult.algorithmResults;
    Object.values(algorithms).forEach(result => {

// 检查算法是否执行成功

        if (result.totalDistance === -1) {

            const errorRow = `
<tr class="table-warning">
<td>${result.algorithmName}</td>
<td colspan="3">算法执行失败：${result.errorMsg || '请检查后端服务日志'}</td>
</tr>
`;

            tableBody.append(errorRow);

            return;

        }

        const endPos = result.processedOrder && result.processedOrder.length > 0
            ? result.processedOrder.at(-1) // 获取最后一个元素作为结束位置
            : 'N/A';
        const row = `
<tr class="hover:bg-gray-50 transition-colors duration-150">
<td class="font-semibold">${result.algorithmName}</td>
<td><span class="font-mono text-sm text-indigo-600">${[0, ...result.processedOrder].join(' → ')}</span></td>
<td><span class="font-bold text-lg text-green-700">${result.totalDistance}</span></td>
<td>${endPos}</td>
</tr>
`;
        tableBody.append(row);
    });

}

/**
 * 显示算法对比（触发渲染并高亮最优算法）
 */
function showAlgorithmComparison() {
    if (!orderScheduleResult) {
        showMessage('请先通过订单ID加载数据！', false);
        return;
    }

// 1. 渲染表格 (确保最新数据已渲染)
    renderAlgorithmComparison();

    const algorithms = orderScheduleResult.algorithmResults;
    if (!algorithms) return;

// 2. 找到总距离最小的算法（最优），排除失败的 (-1)
    let optimalAlg = null;
    let minDistance = Infinity;
    Object.values(algorithms).forEach(result => {
        if (result.totalDistance >= 0 && result.totalDistance < minDistance) {
            minDistance = result.totalDistance;
            optimalAlg = result;
        }
    });

// 3. 高亮最优算法行 - 使用马卡龙绿浅色
    $('#comparison-results tr').each(function(index, row) {
// 假设算法名称在第一列
        const algName = $(row).find('td:first').text();
        if (optimalAlg && algName === optimalAlg.algorithmName) {
            // 使用马卡龙绿浅色高亮
            $(row).removeClass('hover:bg-gray-50').addClass('bg-macaron-green/20 table-success border-l-4 border-macaron-green-dark');
        } else {
            $(row).removeClass('bg-macaron-green/20 table-success border-l-4 border-macaron-green-dark').addClass('hover:bg-gray-50');
        }
    });

    if (optimalAlg) {
        showMessage(`最优算法：${optimalAlg.algorithmName}（总距离${optimalAlg.totalDistance}）`, true);
    } else {
// 如果所有算法都失败或数据异常
        showMessage(`没有找到有效的算法结果进行对比。`, false);
    }

}

// ===================================================================
// 5. 初始化和事件绑定
// ===================================================================

/**
 * 路径规划模块的初始化和事件绑定
 */
function initPathPlanning() {

// 默认展示
    clearLane(ELEMENT_IDS.WAREHOUSE_LANE);
    clearLane(ELEMENT_IDS.REQUEST_LANE);
    clearLane(ELEMENT_IDS.PATH_LANE);

// 绑定加载按钮
    $(document).on('click', `#${ELEMENT_IDS.LOAD_BUTTON}`, function() {
        const orderId = el(ELEMENT_IDS.ORDER_ID_INPUT).value;
        if (orderId) {
            loadOrderData(orderId);
        } else {
            showMessage('请输入订单ID!', false);
        }
    });

// 绑定算法对比按钮
    $(document).on('click', `#${ELEMENT_IDS.COMPARE_BUTTON}`, showAlgorithmComparison);

// 绑定动画控制
    $(document).on('click', `#${ELEMENT_IDS.PLAY_BUTTON}`, playAnimation);
    $(document).on('click', `#${ELEMENT_IDS.PAUSE_BUTTON}`, pauseAnimation);
    $(document).on('click', `#${ELEMENT_IDS.STEP_BUTTON}`, stepAnimation);
    $(document).on('click', `#${ELEMENT_IDS.RESET_BUTTON}`, resetAnimation);
    // 绑定算法选择
    $(document).on('change', `#${ELEMENT_IDS.ALGORITHM_SELECT}`, function() {
        currentAlgorithm = $(this).val(); // 更新 currentAlgorithm

        // 【修改】：暂停当前动画并重置路径显示
        // 动画控制逻辑应该优先处理
        pauseAnimation(); // 确保暂停，防止干扰
        currentStep = 0;

        const result = getCurrentSchedulerResult();
        if (result) {
            renderPathStep(result, currentStep);        // 重新绘制路径图
            renderSchedulerResultStatic(result);     // 重新渲染底下的结果面板
        }

        // 表格是所有算法的对比
        renderAlgorithmComparison(); // <--- 确保对比表格不为空
    });

// 绑定速度选择
    $(document).on('change', `#${ELEMENT_IDS.SPEED_SELECT}`, function() {
        animationSpeed = parseInt($(this).val());
        if (isPlaying) {
            pauseAnimation();
            playAnimation();
        }
    });

    console.log('路径规划模块已初始化');

}

// 页面加载完成后，启动初始化
$(document).ready(function() {
    initPathPlanning();
});