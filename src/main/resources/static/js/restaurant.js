// 全局数据
let orders = [];
let robots = [];
let utensils = [];
let workstations = [];
let algorithmMetrics = {
    withAlgorithm: {
        avgResponseTime: 0,
        throughput: 0
    },
    withoutAlgorithm: {
        avgResponseTime: 0,
        throughput: 0
    }
};

// 内存管理相关数据和逻辑
let memoryManager = {
    // 仅保留获取状态的 API 地址
    API_STATUS: 'http://localhost:8088/api/memory/status',
    
    // 初始化方法只用于加载初始数据和设置定时刷新
    init: function() {
        this.fetchMemoryStatus(); // 首次加载状态
        // 设置定时刷新，用于持续监控
        setInterval(() => this.fetchMemoryStatus(), 1000); // 调整为1秒刷新一次
    },

    // ------------------- API 调用 (仅保留获取状态) -------------------

    // 获取内存状态
    fetchMemoryStatus: async function() {
        try {
            const response = await fetch(this.API_STATUS);
            if (!response.ok) {
                throw new Error('获取内存状态失败');
            }
            const data = await response.json();
            // 收到后端返回的 MemoryVO 数据后，更新可视化
            this.updateMemoryVisualization(data); 
        } catch (error) {
            console.error("Fetch Status Error:", error);
            // 生产环境中不应使用 alert，这里仅作调试提示
            // alert("错误: 无法连接到后端获取内存状态。"); 
        }
    },

    // ------------------- 可视化更新 (基于 MemoryVO 数据) -------------------
    
    // 接收 MemoryVO 对象 (data) 并更新前端显示
    updateMemoryVisualization: function(data) {
        const container = document.getElementById('memory-container');
        const listContainer = document.getElementById('partitions-list');
        
        // 确保数据有效
        if (!data || !data.partitions) return;
        
        const totalSpace = data.totalSpace;
        const usedSpace = data.usedSpace;
        const freeSpace = data.freeSpace;
        const partitions = data.partitions;

        // 计算碎片数量 (JS 端简化计算，空闲分区数量即为碎片数量)
        const freePartitions = partitions.filter(p => !p.allocated);
        const fragmentCount = freePartitions.length;
        const usageRate = (usedSpace / totalSpace) * 100;
        
        // 清空容器
        container.innerHTML = '';
        listContainer.innerHTML = '';
        
        // 1. 更新统计信息
        document.getElementById('total-memory').textContent = totalSpace;
        document.getElementById('used-memory').textContent = usedSpace;
        document.getElementById('free-memory').textContent = freeSpace;
        document.getElementById('fragment-count').textContent = fragmentCount;
        
        // 更新进度条
        const usageBar = document.getElementById('memory-usage-bar');
        usageBar.style.width = `${usageRate}%`;
        usageBar.textContent = `已使用: ${Math.round(usageRate)}%`;

        // 2. 生成分区可视化
        partitions.forEach(partition => {
            const partitionEl = document.createElement('div');
            partitionEl.className = `memory-partition ${partition.allocated ? 'allocated' : 'free'}`;
            
            // 计算宽度百分比
            const widthPercent = (partition.size / totalSpace) * 100;
            partitionEl.style.width = `${widthPercent}%`;
            // 计算起始位置百分比
            partitionEl.style.left = `${(partition.start / totalSpace) * 100}%`;
            
            // 设置显示文本
            if (partition.allocated) {
                // 使用 dishName 或 dishId 进行显示
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
            listItem.innerHTML = `
                <strong>${partition.allocated ? '已分配' : '空闲'}</strong>: 
                起始地址: ${partition.start}KB, 大小: ${partition.size}KB
                ${dishInfo}
            `;
            listContainer.appendChild(listItem);
        });
    }
};



// 页面加载完成后初始化
$(document).ready(function() {
    // 初始化导航切换
    initNavigation();
    
    // 初始化模拟数据
    initMockData();
    
    // --- 【内存管理器初始化】 ---
    
    // 检查是否存在可视化容器，并初始化 memoryManager
    if (document.getElementById('memory-page')) {
        // 调用 memoryManager 的 init 方法，它会启动后端状态获取和定时刷新
        memoryManager.init();
    }

    
    // 绑定内存操作按钮事件
    bindMemoryEvents();
    
    // 渲染各页面数据
    renderDashboard();
    renderOrders();
    renderRobots();
    renderResources();
    
    // 设置定时刷新
    setInterval(function() {
        updateMockData();
        renderDashboard();
        renderOrders();
        renderRobots();
        renderResources();
        updateRobotPositions();
        
        // 定期从后端获取算法指标数据
        fetchAlgorithmMetrics();
    }, 5000); // 每5秒刷新一次
    
    // 初始获取算法指标
    fetchAlgorithmMetrics();
});

// 绑定内存操作事件
function bindMemoryEvents() {
    // 分配内存按钮
    $('#allocate-btn').click(function() {
        const dishId = $('#dish-id').val().trim();
        const size = parseInt($('#dish-size').val());
        
        const result = memoryManager.allocateForDish(dishId, size);
        showMemoryMessage(result.message, result.success);
    });
    
    // 释放内存按钮
    $('#release-btn').click(function() {
        const dishId = $('#release-dish-id').val().trim();
        const result = memoryManager.releaseDishPartition(dishId);
        showMemoryMessage(result.message, result.success);
    });
    
    // 碎片整理按钮
    $('#defrag-btn').click(function() {
        const result = memoryManager.defragmentSpace();
        showMemoryMessage(result.message, result.success);
    });
    
    // 内存页面导航
    $('#nav-memory').click(function(e) {
        e.preventDefault();
        showPage('memory-page');
        setActiveNav('nav-memory');
    });
}

// 显示内存操作消息
function showMemoryMessage(message, isSuccess) {
    // 创建临时消息元素
    const messageEl = document.createElement('div');
    messageEl.className = `alert ${isSuccess ? 'alert-success' : 'alert-danger'} position-fixed top-20 end-3 z-50`;
    messageEl.textContent = message;
    messageEl.style.maxWidth = '300px';
    
    // 添加到页面
    document.body.appendChild(messageEl);
    
    // 3秒后移除
    setTimeout(() => {
        messageEl.classList.add('fade-out');
        setTimeout(() => {
            document.body.removeChild(messageEl);
        }, 500);
    }, 3000);
}

// 从后端获取算法指标数据
function fetchAlgorithmMetrics() {
    // 模拟后端数据返回
    simulateAlgorithmMetrics();
    updateAlgorithmMetricsUI();
}

// 模拟算法指标数据
function simulateAlgorithmMetrics() {
    // 模拟使用算法的情况 - 更好的性能
    algorithmMetrics.withAlgorithm = {
        avgResponseTime: (3 + Math.random() * 2).toFixed(1), // 3-5分钟
        throughput: (25 + Math.random() * 10).toFixed(0)     // 25-35单/小时
    };
    
    // 模拟不使用算法的情况 - 较差的性能
    algorithmMetrics.withoutAlgorithm = {
        avgResponseTime: (6 + Math.random() * 3).toFixed(1), // 6-9分钟
        throughput: (15 + Math.random() * 5).toFixed(0)      // 15-20单/小时
    };
}

// 更新算法指标UI
function updateAlgorithmMetricsUI() {
    // 更新指标卡片
    $('#alg-avg-response').text(algorithmMetrics.withAlgorithm.avgResponseTime);
    $('#noalg-avg-response').text(algorithmMetrics.withoutAlgorithm.avgResponseTime);
    
    $('#alg-throughput').text(algorithmMetrics.withAlgorithm.throughput);
    $('#noalg-throughput').text(algorithmMetrics.withoutAlgorithm.throughput);
    
    // 计算改进百分比
    const responseImprovement = Math.round((1 - 
        algorithmMetrics.withAlgorithm.avgResponseTime / algorithmMetrics.withoutAlgorithm.avgResponseTime) * 100);
    const throughputImprovement = Math.round((
        algorithmMetrics.withAlgorithm.throughput / algorithmMetrics.withoutAlgorithm.throughput - 1) * 100);
    
    // 更新改进百分比并设置颜色
    $('#response-improvement').text(responseImprovement + '%').removeClass('improvement-positive improvement-negative')
        .addClass(responseImprovement > 0 ? 'improvement-positive' : 'improvement-negative');
    $('#throughput-improvement').text(throughputImprovement + '%').removeClass('improvement-positive improvement-negative')
        .addClass(throughputImprovement > 0 ? 'improvement-positive' : 'improvement-negative');
    
    // 渲染对比图表
    renderAlgorithmComparisonChart();
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
    
    // 提交新订单
    $('#submit-order').click(function() {
        createNewOrder();
        $('#new-order-modal').modal('hide');
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

// 初始化模拟数据
function initMockData() {
    // 初始化订单数据
    const now = new Date();
    for (let i = 1; i <= 8; i++) {
        const createTime = new Date(now - i * 60000 * (5 + Math.random() * 15));
        const statuses = ['pending', 'cooking', 'delivering', 'completed'];
        const status = statuses[Math.floor(Math.random() * (i < 3 ? 3 : 4))];
        const algorithmType = Math.random() > 0.5 ? 'better' : 'basic';
        
        orders.push({
            id: 'ORD' + now.getFullYear() + (now.getMonth() + 1) + now.getDate() + i,
            dishes: getRandomDishes(),
            createTime: formatDateTime(createTime),
            priority: Math.floor(Math.random() * 5) + 1,
            status: status,
            robotId: status !== 'pending' ? 'RB' + (Math.floor(Math.random() * 5) + 1) : null,
            algorithmType: algorithmType,
            algorithmText: algorithmType === 'better' ? '优化算法' : '基础算法',
            completeTime: status === 'completed' ? formatDateTime(new Date(createTime.getTime() + 100000 + Math.random() * 300000)) : null
        });
    }
    
    // 初始化机器人数据（只保留忙碌和空闲两种状态，不区分类型）
    for (let i = 1; i <= 5; i++) {
        const statuses = ['idle', 'busy'];
        const status = statuses[Math.floor(Math.random() * 2)];
        
        robots.push({
            id: 'RB' + i,
            name: '机器人' + i, // 统一命名为"机器人+ID"
            status: status,
            statusText: status === 'idle' ? '空闲' : '忙碌',
            battery: Math.floor(Math.random() * 40) + 60,
            currentTask: status === 'busy' ? 'ORD' + (Math.floor(Math.random() * 8) + 1) : null,
            location: { x: 50 + Math.random() * 600, y: 50 + Math.random() * 350 },
            completedOrders: Math.floor(Math.random() * 20) + 5
        });
    }
    
    // 初始化烹饪器具数据
    const utensilTypes = ['炒锅', '烤箱', '蒸锅', '煎锅', '微波炉'];
    for (let i = 1; i <= 6; i++) {
        const statuses = ['available', 'occupied'];
        const status = statuses[Math.floor(Math.random() * 2)];
        
        utensils.push({
            id: 'UT' + i,
            type: utensilTypes[Math.floor(Math.random() * utensilTypes.length)],
            status: status,
            statusText: status === 'available' ? '空闲' : '占用中',
            robotId: status === 'occupied' ? 'RB' + (Math.floor(Math.random() * 5) + 1) : null
        });
    }
    
    // 初始化工作台数据
    for (let i = 1; i <= 4; i++) {
        const statuses = ['available', 'occupied'];
        const status = statuses[Math.floor(Math.random() * 2)];
        
        workstations.push({
            id: 'WS' + i,
            capacity: 2 + Math.floor(Math.random() * 3),
            status: status,
            statusText: status === 'available' ? '空闲' : '占用中',
            robotId: status === 'occupied' ? 'RB' + (Math.floor(Math.random() * 5) + 1) : null,
            currentTask: status === 'occupied' ? 'ORD' + (Math.floor(Math.random() * 8) + 1) : null
        });
    }
    
    // 初始化餐厅地图
    initRestaurantMap();
}

// 初始化餐厅地图
function initRestaurantMap() {
    const map = $('#restaurant-map');
    
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
    
    // 添加机器人标记（统一颜色）
    robots.forEach(robot => {
        map.append(`<div class="robot-marker" 
                    style="left: ${robot.location.x}px; top: ${robot.location.y}px;" 
                    id="robot-${robot.id}">${robot.id.substring(2)}</div>`);
    });
}

// 更新模拟数据（模拟实时变化）
function updateMockData() {
    // 更新订单状态
    orders.forEach(order => {
        if (order.status === 'pending' && Math.random() > 0.7) {
            order.status = 'cooking';
            order.robotId = 'RB' + (Math.floor(Math.random() * 5) + 1);
        } else if (order.status === 'cooking' && Math.random() > 0.8) {
            order.status = 'delivering';
            // 可能更换机器人
            if (Math.random() > 0.5) {
                order.robotId = 'RB' + (Math.floor(Math.random() * 5) + 1);
            }
        } else if (order.status === 'delivering' && Math.random() > 0.7) {
            order.status = 'completed';
            order.completeTime = formatDateTime(new Date());
        }
        
        // 动态调整优先级（每20秒增加1）
        if (['pending', 'cooking', 'delivering'].includes(order.status) && Math.random() > 0.7) {
            order.priority = Math.min(5, order.priority + 1);
        }
    });
    
    // 更新机器人状态（只保留忙碌和空闲）
    robots.forEach(robot => {
        // 忙碌机器人完成任务
        if (robot.status === 'busy' && Math.random() > 0.8) {
            robot.status = 'idle';
            robot.statusText = '空闲';
            robot.currentTask = null;
        }
        
        // 空闲机器人分配新任务
        if (robot.status === 'idle' && Math.random() > 0.7) {
            const pendingOrders = orders.filter(o => o.status === 'pending' || o.status === 'cooking');
            if (pendingOrders.length > 0) {
                robot.status = 'busy';
                robot.statusText = '忙碌';
                robot.currentTask = pendingOrders[Math.floor(Math.random() * pendingOrders.length)].id;
            }
        }
        
        // 更新位置（随机小幅移动）
        if (robot.status === 'busy') {
            robot.location.x = Math.max(50, Math.min(650, robot.location.x + (Math.random() - 0.5) * 30));
            robot.location.y = Math.max(50, Math.min(400, robot.location.y + (Math.random() - 0.5) * 30));
        }
    });
    
    // 更新资源状态
    utensils.forEach(utensil => {
        if (utensil.status === 'occupied' && Math.random() > 0.7) {
            utensil.status = 'available';
            utensil.statusText = '空闲';
            utensil.robotId = null;
        } else if (utensil.status === 'available' && Math.random() > 0.6) {
            utensil.status = 'occupied';
            utensil.statusText = '占用中';
            utensil.robotId = 'RB' + (Math.floor(Math.random() * 5) + 1);
        }
    });
    
    workstations.forEach(station => {
        if (station.status === 'occupied' && Math.random() > 0.75) {
            station.status = 'available';
            station.statusText = '空闲';
            station.robotId = null;
            station.currentTask = null;
        } else if (station.status === 'available' && Math.random() > 0.65) {
            station.status = 'occupied';
            station.statusText = '占用中';
            station.robotId = 'RB' + (Math.floor(Math.random() * 5) + 1);
            const pendingOrders = orders.filter(o => o.status === 'pending' || o.status === 'cooking');
            station.currentTask = pendingOrders.length > 0 ? pendingOrders[Math.floor(Math.random() * pendingOrders.length)].id : null;
        }
    });
}

// 更新机器人位置
function updateRobotPositions() {
    robots.forEach(robot => {
        $(`#robot-${robot.id}`).css({
            left: robot.location.x + 'px',
            top: robot.location.y + 'px'
        });
    });
}

// 渲染仪表盘
function renderDashboard() {
    // 统计数据
    const todayOrders = orders.length;
    const completedOrders = orders.filter(o => o.status === 'completed').length;
    const completionRate = todayOrders > 0 ? Math.round((completedOrders / todayOrders) * 100) : 0;
    const activeRobots = robots.filter(r => r.status === 'busy').length;
    const totalRobots = robots.length;
    
    // 计算平均出餐时间
    let avgTime = 0;
    const completed = orders.filter(o => o.status === 'completed');
    if (completed.length > 0) {
        completed.forEach(order => {
            const create = new Date(order.createTime);
            const complete = new Date(order.completeTime);
            avgTime += (complete - create) / (1000 * 60); // 转换为分钟
        });
        avgTime = Math.round((avgTime / completed.length) * 10) / 10;
    }
    
    // 更新统计卡片
    $('#today-orders').text(todayOrders);
    $('#completed-orders').text(completedOrders);
    $('#completion-rate').text(completionRate + '%');
    $('#active-robots').text(activeRobots);
    $('#total-robots').text(totalRobots);
    $('#avg-time').text(avgTime);
    
    // 渲染订单趋势图表
    renderOrdersChart();
}

// 渲染订单趋势图表
function renderOrdersChart() {
    const ctx = document.getElementById('orders-chart').getContext('2d');
    
    // 按小时统计今天的订单
    const hours = Array(24).fill(0);
    const now = new Date();
    const today = now.toDateString();
    
    orders.forEach(order => {
        const orderDate = new Date(order.createTime);
        if (orderDate.toDateString() === today) {
            const hour = orderDate.getHours();
            hours[hour]++;
        }
    });
    
    // 只显示有数据的小时
    const labels = [];
    const data = [];
    hours.forEach((count, index) => {
        if (count > 0 || index === now.getHours()) { // 确保显示当前小时
            labels.push(index + ':00');
            data.push(count);
        }
    });
    
    // 销毁已存在的图表
    if (window.ordersChart) {
        window.ordersChart.destroy();
    }
    
    // 创建新图表 - 使用马卡龙色系
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
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        color: 'rgba(240, 230, 224, 0.7)'
                    }
                },
                x: {
                    grid: {
                        color: 'rgba(240, 230, 224, 0.7)'
                    }
                }
            }
        }
    });
}

// 渲染订单列表
function renderOrders() {
    const tableBody = $('#orders-table-body');
    const searchTerm = $('#order-search').val().toLowerCase();
    const statusFilter = $('#order-status-filter').val();
    const priorityFilter = $('#order-priority-filter').val();
    
    // 筛选订单
    let filteredOrders = orders.filter(order => {
        const matchesSearch = order.id.toLowerCase().includes(searchTerm);
        const matchesStatus = statusFilter === 'all' || order.status === statusFilter;
        const matchesPriority = priorityFilter === 'all' || order.priority.toString() === priorityFilter;
        return matchesSearch && matchesStatus && matchesPriority;
    });
    
    // 按创建时间排序（最新的在前）
    filteredOrders.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
    
    // 清空表格
    tableBody.empty();
    
    // 显示无数据消息或填充表格
    if (filteredOrders.length === 0) {
        $('#no-orders-message').removeClass('d-none');
        return;
    }
    
    $('#no-orders-message').addClass('d-none');
    
    // 填充表格 - 使用马卡龙色系状态标签
    filteredOrders.forEach(order => {
        // 状态样式映射（马卡龙色系）
        const statusMap = {
            'pending': { text: '待处理', style: 'background-color: var(--macaron-purple); color: #4B3B47;' },
            'cooking': { text: '烹饪中', style: 'background-color: var(--macaron-pink); color: #D64933;' },
            'delivering': { text: '配送中', style: 'background-color: var(--macaron-blue); color: #2A7B9B;' },
            'completed': { text: '已完成', style: 'background-color: var(--macaron-green); color: #4A5859;' },
            'cancelled': { text: '已取消', style: 'background-color: #F4B393; color: #A43820;' }
        };
        
        // 优先级样式（马卡龙色系）
        let priorityStyle = '';
        if (order.priority >= 4) priorityStyle = 'background-color: var(--macaron-pink); color: #D64933;';
        else if (order.priority >= 3) priorityStyle = 'background-color: var(--macaron-orange); color: #D35400;';
        else if (order.priority >= 2) priorityStyle = 'background-color: var(--macaron-green); color: #4A5859;';
        else priorityStyle = 'background-color: var(--macaron-blue); color: #2A7B9B;';
        
        // 算法类型样式
        const algorithmStyle = order.algorithmType === 'better' 
            ? 'background-color: var(--macaron-green); color: #4A5859;' 
            : 'background-color: var(--macaron-purple); color: #4B3B47;';
        
        const row = `
            <tr>
                <td>${order.id}</td>
                <td>${order.dishes.join(', ')}</td>
                <td>${order.createTime}</td>
                <td><span class="status-badge" style="${priorityStyle}">${order.priority}</span></td>
                <td><span class="status-badge" style="${statusMap[order.status].style}">${statusMap[order.status].text}</span></td>
                <td>${order.robotId ? '机器人' + order.robotId.substring(2) : '-'}</td> <!-- 显示统一的机器人名称 -->
                <td><span class="status-badge" style="${algorithmStyle}">${order.algorithmText}</span></td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn" style="background-color: var(--macaron-blue); color: #2A7B9B;" title="查看详情">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${order.status === 'pending' ? `
                            <button class="btn" style="background-color: var(--macaron-orange); color: #D35400;" title="修改优先级">
                                <i class="fas fa-exclamation-circle"></i>
                            </button>
                            <button class="btn" style="background-color: #F4B393; color: #A43820;" title="取消订单">
                                <i class="fas fa-times"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
        
        tableBody.append(row);
    });
}

// 渲染机器人监控页面
function renderRobots() {
    const container = $('#robots-container');
    
    // 清空容器
    container.empty();
    
    // 填充机器人卡片 - 使用马卡龙色系（统一类型，只有忙碌和空闲状态）
    robots.forEach(robot => {
        // 状态样式映射（马卡龙色系）
        const statusMap = {
            'idle': { style: 'background-color: var(--macaron-green); color: #4A5859;', icon: 'check-circle' },
            'busy': { style: 'background-color: var(--macaron-orange); color: #D35400;', icon: 'cog' }
        };
        
        const card = `
            <div class="col-md-4 col-lg-3 mb-4">
                <div class="card robot-card h-100">
                    <div class="card-header" style="background-color: var(--macaron-blue); color: #2A7B9B;">
                        <div class="d-flex justify-content-between align-items-center">
                            <h5 class="mb-0">${robot.name}</h5> <!-- 使用统一名称 -->
                            <i class="fas fa-robot"></i>
                        </div>
                    </div>
                    <div class="card-body">
                        <p class="card-text"><strong>状态：</strong>
                            <span class="status-badge" style="${statusMap[robot.status].style}">
                                <i class="fas fa-${statusMap[robot.status].icon} me-1"></i>${robot.statusText}
                            </span>
                        </p>
                        <p class="card-text"><strong>当前任务：</strong>${robot.currentTask || '无'}</p>
                        <p class="card-text"><strong>已完成订单：</strong>${robot.completedOrders}</p>
                    </div>
                </div>
            </div>
        `;
        
        container.append(card);
    });
    
    // 渲染机器人状态图表（只显示忙碌和空闲）
    renderRobotsStatusChart();
    
    // 渲染机器人工作量图表
    renderRobotsWorkloadChart();
}

// 渲染机器人状态分布图表（只显示忙碌和空闲）
function renderRobotsStatusChart() {
    const ctx = document.getElementById('robots-status-chart').getContext('2d');
    
    // 统计各状态机器人数量（只统计忙碌和空闲）
    const statusData = {
        '空闲': robots.filter(r => r.status === 'idle').length,
        '忙碌': robots.filter(r => r.status === 'busy').length
    };
    
    // 销毁已存在的图表
    if (window.robotsStatusChart) {
        window.robotsStatusChart.destroy();
    }
    
    // 创建新图表 - 使用马卡龙色系
    window.robotsStatusChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(statusData),
            datasets: [{
                data: Object.values(statusData),
                backgroundColor: [
                    'rgba(199, 206, 234, 0.8)', // 马卡龙绿
                    'rgba(244, 171, 119, 0.8)'  // 马卡龙蓝
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

// 渲染机器人工作量图表
function renderRobotsWorkloadChart() {
    const ctx = document.getElementById('robots-workload-chart').getContext('2d');
    
    // 按机器人ID排序
    const sortedRobots = [...robots].sort((a, b) => a.id.localeCompare(b.id));
    
    // 销毁已存在的图表
    if (window.robotsWorkloadChart) {
        window.robotsWorkloadChart.destroy();
    }
    
    // 创建新图表 - 使用马卡龙色系
    window.robotsWorkloadChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: sortedRobots.map(r => r.name), // 使用统一名称作为标签
            datasets: [{
                label: '已完成订单数',
                data: sortedRobots.map(r => r.completedOrders),
                backgroundColor: 'rgba(255, 202, 212, 0.7)', // 马卡龙粉
                borderColor: 'rgba(214, 73, 51, 0.8)',      // 深粉色
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        color: 'rgba(240, 230, 224, 0.7)'
                    }
                },
                x: {
                    grid: {
                        color: 'rgba(240, 230, 224, 0.7)'
                    }
                }
            }
        }
    });
}

// 渲染资源管理页面
function renderResources() {
    // 渲染烹饪器具表格
    const utensilsTable = $('#utensils-table-body');
    utensilsTable.empty();
    
    utensils.forEach(utensil => {
        const statusStyle = utensil.status === 'available' 
            ? 'background-color: var(--macaron-green); color: #4A5859;' 
            : 'background-color: var(--macaron-pink); color: #D64933;';
        
        const row = `
            <tr>
                <td>${utensil.id}</td>
                <td>${utensil.type}</td>
                <td><span class="status-badge" style="${statusStyle}">${utensil.statusText}</span></td>
                <td>${utensil.robotId ? '机器人' + utensil.robotId.substring(2) : '-'}</td>
            </tr>
        `;
        
        utensilsTable.append(row);
    });
    
    // 渲染工作台表格
    const workstationsTable = $('#workstations-table-body');
    workstationsTable.empty();
    
    workstations.forEach(station => {
        const statusStyle = station.status === 'available' 
            ? 'background-color: var(--macaron-green); color: #4A5859;' 
            : 'background-color: var(--macaron-pink); color: #D64933;';
        
        const row = `
            <tr>
                <td>${station.id}</td>
                <td>${station.capacity} 单位</td>
                <td><span class="status-badge" style="${statusStyle}">${station.statusText}</span></td>
                <td>${station.robotId ? '机器人' + station.robotId.substring(2) : '-'}</td>
                <td>${station.currentTask || '-'}</td>
            </tr>
        `;
        
        workstationsTable.append(row);
    });
}

// 渲染算法对比图表
function renderAlgorithmComparisonChart() {
    const ctx = document.getElementById('algorithm-comparison-chart').getContext('2d');

    // 销毁已存在的图表
    if (window.algorithmComparisonChart) {
        window.algorithmComparisonChart.destroy();
    }

    // 准备图表数据
    const labels = ['平均响应时间 (分钟)', '吞吐量 (单/小时)'];
    const dataWithAlgorithm = [
        parseFloat(algorithmMetrics.withAlgorithm.avgResponseTime),
        parseFloat(algorithmMetrics.withAlgorithm.throughput)
    ];
    const dataWithoutAlgorithm = [
        parseFloat(algorithmMetrics.withoutAlgorithm.avgResponseTime),
        parseFloat(algorithmMetrics.withoutAlgorithm.throughput)
    ];

    // 创建新图表 - 使用马卡龙色系
    window.algorithmComparisonChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: '使用优化算法',
                    data: dataWithAlgorithm,
                    backgroundColor: 'rgba(168, 218, 220, 0.7)', // 马卡龙蓝
                    borderColor: 'rgba(42, 123, 155, 0.8)',      // 深蓝色
                    borderWidth: 1
                },
                {
                    label: '不使用算法',
                    data: dataWithoutAlgorithm,
                    backgroundColor: 'rgba(255, 202, 212, 0.7)', // 马卡龙粉
                    borderColor: 'rgba(214, 73, 51, 0.8)',      // 深粉色
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(240, 230, 224, 0.7)'
                    }
                },
                x: {
                    grid: {
                        color: 'rgba(240, 230, 224, 0.7)'
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed.y !== null) {
                                if (context.dataIndex === 0) {
                                    label += context.parsed.y + ' 分钟';
                                } else {
                                    label += context.parsed.y + ' 单/小时';
                                }
                            }
                            return label;
                        }
                    }
                }
            }
        }
    });
}

// 创建新订单
function createNewOrder() {
    // 获取选中的菜品
    const selectedDishes = [];
    $('.form-check-input:checked').each(function() {
        selectedDishes.push($(this).val());
    });
    
    // 验证是否选择了菜品
    if (selectedDishes.length === 0) {
        alert('请至少选择一道菜品');
        return;
    }
    
    // 获取优先级和算法类型
    const priority = parseInt($('#order-priority').val());
    const algorithmType = $('#order-processing-algorithm').val();
    
    // 生成订单ID
    const now = new Date();
    const orderId = 'ORD' + now.getFullYear() + (now.getMonth() + 1) + now.getDate() + (orders.length + 1);
    
    // 创建新订单对象
    const newOrder = {
        id: orderId,
        dishes: selectedDishes,
        createTime: formatDateTime(now),
        priority: priority,
        status: 'pending',
        robotId: null,
        algorithmType: algorithmType,
        algorithmText: algorithmType === 'better' ? '优化算法' : '基础算法',
        completeTime: null
    };
    
    // 添加到订单列表
    orders.unshift(newOrder);
    
    // 重新渲染订单页面
    renderOrders();
    renderDashboard();
    
    // 重置表单
    $('#new-order-form')[0].reset();
    
    // 显示成功消息
    alert('订单创建成功！订单号：' + orderId);
}

// 辅助函数：生成随机菜品列表
function getRandomDishes() {
    const dishes = ['鱼香肉丝', '宫保鸡丁', '西红柿炒鸡蛋', '麻婆豆腐', '米饭', '炒时蔬', '酸辣汤'];
    const selected = [];
    const count = Math.floor(Math.random() * 3) + 1; // 1-3种菜品
    
    for (let i = 0; i < count; i++) {
        let dish;
        do {
            dish = dishes[Math.floor(Math.random() * dishes.length)];
        } while (selected.includes(dish));
        selected.push(dish);
    }
    
    return selected;
}

// 辅助函数：格式化日期时间
function formatDateTime(date) {
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    }).replace(',', ' ');
}
    