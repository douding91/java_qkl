#!/bin/bash

# 加载用户配置
source ~/.zshrc

# 设置颜色输出
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 输出带颜色的文本函数
print_green() {
    echo -e "\033[32m$1\033[0m"
}

print_yellow() {
    echo -e "\033[33m$1\033[0m"
}

print_red() {
    echo -e "\033[31m$1\033[0m"
}

# 检查并设置Node.js环境
setup_node() {
    print_yellow "设置Node.js环境..."
    
    # 检查是否安装了node
    if ! command -v node &> /dev/null; then
        print_red "未找到Node.js，请先安装Node.js 20或更高版本"
        exit 1
    fi
    
    # 检查node版本
    NODE_VERSION=$(node -v)
    print_green "当前使用的Node.js版本: $NODE_VERSION"
    
    # 检查是否安装了npm
    if ! command -v npm &> /dev/null; then
        print_red "未找到npm，请先安装npm"
        exit 1
    fi
    
    # 检查是否安装了truffle
    if ! command -v truffle &> /dev/null; then
        print_yellow "安装truffle..."
        npm install -g truffle
    fi
    
    # 检查是否安装了ganache-cli
    if ! command -v ganache-cli &> /dev/null; then
        print_yellow "安装ganache-cli..."
        npm install -g ganache-cli@6.4.4
    fi
    
    print_green "所有必要的工具已就绪"
}

# 检查必要的工具是否安装
check_requirements() {
    print_yellow "检查必要的工具..."
    
    # 设置Node.js环境
    setup_node
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        print_red "未安装Maven，请先安装Maven"
        exit 1
    fi
    
    print_green "所有必要的工具已就绪"
}

# 检查Ganache是否正在运行
check_ganache() {
    nc -z localhost 7545 2>/dev/null
    return $?
}

# 启动Ganache
start_ganache() {
    print_yellow "启动Ganache..."
    
    # 如果Ganache已经在运行，先关闭它
    if check_ganache; then
        print_yellow "Ganache已经在运行，正在关闭..."
        pkill -f "ganache-cli"
        sleep 2
    fi
    
    # 启动Ganache
    ganache-cli --port 7545 --networkId 1337 --chainId 1337 --deterministic &
    GANACHE_PID=$!
    
    # 等待Ganache启动
    print_yellow "检查Ganache状态..."
    ATTEMPTS=0
    MAX_ATTEMPTS=30
    while ! check_ganache; do
        if [ $ATTEMPTS -ge $MAX_ATTEMPTS ]; then
            print_red "Ganache启动超时"
            exit 1
        fi
        ATTEMPTS=$((ATTEMPTS + 1))
        print_yellow "等待Ganache启动... (尝试 $ATTEMPTS/$MAX_ATTEMPTS)"
        sleep 1
    done
    
    print_green "Ganache正在运行"
    print_green "Ganache已启动，运行在端口7545"
    
    # 等待Ganache初始化账户
    print_yellow "等待Ganache初始化账户..."
    sleep 2
}

# 部署智能合约
deploy_contracts() {
    print_yellow "部署智能合约..."
    
    # 检查Ganache状态
    if ! check_ganache; then
        print_red "Ganache未运行，无法部署合约"
        exit 1
    fi
    
    # 创建日志目录
    mkdir -p logs
    
    # 编译合约
    print_yellow "编译智能合约..."
    truffle compile
    
    # 部署合约并保存输出到日志文件
    print_yellow "开始部署合约..."
    truffle migrate --reset > logs/truffle.log 2>&1
    DEPLOY_STATUS=$?
    
    # 显示部署日志
    cat logs/truffle.log
    
    # 检查部署是否成功
    if [ $DEPLOY_STATUS -eq 0 ]; then
        print_green "智能合约部署成功"
        
        # 从部署输出中提取合约地址，使用 "contract address:" 行
        CONTRACT_ADDRESS=$(grep -i "contract address:" logs/truffle.log | grep -o "0x[a-fA-F0-9]\{40\}")
        
        if [ -n "$CONTRACT_ADDRESS" ]; then
            print_green "获取到新的合约地址: $CONTRACT_ADDRESS"
            # 设置环境变量
            export CONTRACT_ADDRESS
            # 设置默认的所有者私钥（第一个账户的私钥）
            export OWNER_PRIVATE_KEY="0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d"
            # 将环境变量写入临时文件，供其他进程使用
            echo "CONTRACT_ADDRESS=$CONTRACT_ADDRESS" > .env
            echo "OWNER_PRIVATE_KEY=$OWNER_PRIVATE_KEY" >> .env
            # 加载环境变量
            source .env
            print_green "环境变量已设置："
            echo "CONTRACT_ADDRESS=$CONTRACT_ADDRESS"
            echo "OWNER_PRIVATE_KEY=$OWNER_PRIVATE_KEY"
        else
            print_red "无法获取合约地址"
            print_yellow "部署日志内容："
            cat logs/truffle.log | grep -i "contract"
            exit 1
        fi
    else
        print_red "智能合约部署失败"
        print_yellow "部署日志内容："
        cat logs/truffle.log
        exit 1
    fi
}

# 启动Spring Boot应用
start_spring_boot() {
    print_yellow "启动Spring Boot应用..."
    
    # 检查端口8088是否被占用
    if lsof -Pi :8088 -sTCP:LISTEN -t >/dev/null ; then
        print_yellow "端口8088已被占用，正在关闭占用该端口的进程..."
        # 获取占用8088端口的进程PID
        PID=$(lsof -ti:8088)
        if [ ! -z "$PID" ]; then
            # 关闭进程
            kill -9 $PID
            print_green "已关闭占用8088端口的进程 (PID: $PID)"
            # 等待端口释放
            sleep 2
        fi
    fi
    
    # 检查并加载环境变量
    if [ -f .env ]; then
        print_yellow "加载环境变量..."
        source .env
        # 将环境变量导出到当前会话
        export CONTRACT_ADDRESS
        export OWNER_PRIVATE_KEY
        print_green "环境变量已加载："
        echo "CONTRACT_ADDRESS=$CONTRACT_ADDRESS"
        echo "OWNER_PRIVATE_KEY=$OWNER_PRIVATE_KEY"
        # 使用 env 命令启动 Spring Boot 应用，确保环境变量可用
        env CONTRACT_ADDRESS="$CONTRACT_ADDRESS" OWNER_PRIVATE_KEY="$OWNER_PRIVATE_KEY" mvn spring-boot:run -DskipTests > logs/spring-boot.log 2>&1 &
    else
        print_red "找不到环境变量文件 .env"
        exit 1
    fi
    
    SPRING_PID=$!
    
    # 等待应用启动
    sleep 10
    
    # 检查应用是否成功启动
    if ps -p $SPRING_PID > /dev/null; then
        print_green "Spring Boot应用已启动，访问 http://localhost:8088"
        print_green "H2 控制台可访问 http://localhost:8088/h2-console"
    else
        print_red "Spring Boot应用启动失败，请检查logs/spring-boot.log"
        exit 1
    fi
}

# 创建日志目录
mkdir -p logs

# 主流程
print_yellow "=== 区块链简历验证系统启动脚本 ==="
echo

# 检查必要的工具
check_requirements

# 启动所有服务
start_ganache
deploy_contracts
start_spring_boot

print_green "=== 所有服务已启动 ==="
echo "Ganache: http://localhost:7545"
echo "Spring Boot应用: http://localhost:8088"
echo "H2数据库控制台: http://localhost:8088/h2-console"
echo
print_yellow "提示："
echo "1. 查看Ganache日志: tail -f logs/ganache.log"
echo "2. 查看Spring Boot应用日志: tail -f logs/spring-boot.log"
echo "3. 停止所有服务: pkill -f ganache && pkill -f spring-boot" 