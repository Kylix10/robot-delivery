from flask import Flask, jsonify, request
import random

app = Flask(__name__)

# --- 模拟数据源 ---
warehouse_data = {i * 10: f"食材{i+1}" for i in range(10)}

def generate_requests(count=6):
    return [random.randint(0, 100) for _ in range(count)]

def schedule_algorithm(algorithm, requests, initial=0):
    processed = requests.copy()
    if algorithm == "SSTF":
        processed.sort(key=lambda x: abs(x - initial))
    elif algorithm == "SCAN":
        processed.sort()
    total = 0
    cur = initial
    step = []
    for r in processed:
        d = abs(r - cur)
        total += d
        step.append(d)
        cur = r
    return {
        "algorithmName": algorithm,
        "processedOrder": processed,
        "stepDistances": step,
        "totalDistance": total
    }

# --- 模拟接口 ---
@app.route('/api/warehouse/ingredients')
def warehouse():
    return jsonify(warehouse_data)

@app.route('/api/warehouse/random-requests')
def random_requests():
    count = int(request.args.get("count", 6))
    return jsonify(generate_requests(count))

@app.route('/api/schedule')
def schedule():
    algorithm = request.args.get("algorithm", "FCFS")
    initial = int(request.args.get("initialPosition", 0))
    # 模拟用随机请求生成结果
    requests = generate_requests(6)
    return jsonify(schedule_algorithm(algorithm, requests, initial))

@app.route('/order/recent')
def recent_orders():
    limit = int(request.args.get("limit", 5))
    orders = [
        {
            "orderId": 1000 + i,
            "orderName": f"订单{i+1}",
            "dish": {"dishId": 500 + i, "dishName": f"菜品{i+1}"}
        } for i in range(limit)
    ]
    return jsonify(orders)

@app.route('/api/order-scheduler/dish/<int:dish_id>')
def dish_schedule(dish_id):
    initial = int(request.args.get("initialPosition", 0))
    reqs = generate_requests(5)
    algos = ["FCFS", "SSTF", "SCAN"]
    result = {a: schedule_algorithm(a, reqs, initial) for a in algos}
    return jsonify({"algorithmResults": result})

if __name__ == '__main__':
    print("✅ Mock server running at http://127.0.0.1:5000")
    app.run(port=5000)

from flask import send_from_directory
import os

@app.route('/')
def serve_index():
    return send_from_directory('.', 'index.html')



