#!/bin/bash

# 设置颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}===== 区块链简历验证系统部署脚本 =====${NC}"

# 确保Ganache运行中
if ! pgrep -f "ganache" > /dev/null; then
    echo -e "${YELLOW}Ganache未运行，请先启动Ganache${NC}"
    echo -e "运行命令：npx ganache-cli --port 7545"
    exit 1
fi

# 关闭可能占用端口的进程
echo -e "${YELLOW}检查并关闭可能占用8088端口的进程...${NC}"
PORT_PROCESS=$(lsof -i :8088 | grep LISTEN | awk '{print $2}')
if [ ! -z "$PORT_PROCESS" ]; then
    echo -e "${YELLOW}发现进程 $PORT_PROCESS 占用端口8088，尝试关闭...${NC}"
    kill -9 $PORT_PROCESS
    sleep 2
    if lsof -i :8088 | grep LISTEN > /dev/null; then
        echo -e "${RED}无法关闭占用8088端口的进程，请手动关闭后重试${NC}"
        exit 1
    else
        echo -e "${GREEN}成功关闭占用端口的进程${NC}"
    fi
else
    echo -e "${GREEN}端口8088未被占用${NC}"
fi

# 清理并跳过测试编译
echo -e "${GREEN}编译项目（跳过测试）...${NC}"
mvn clean package -Dmaven.test.skip=true

if [ $? -ne 0 ]; then
    echo -e "${RED}编译失败，请检查错误信息${NC}"
    exit 1
fi

# 提取最新合约地址
echo -e "${GREEN}获取最新合约地址...${NC}"
CONTRACT_ADDRESS=$(grep -A2 "contract created" logs/ganache.log | tail -1 | grep -o "0x[a-fA-F0-9]\{40\}" || echo "")
if [ ! -z "$CONTRACT_ADDRESS" ]; then
    echo -e "${GREEN}从日志中获取到最新合约地址: ${CONTRACT_ADDRESS}${NC}"
    export CONTRACT_ADDRESS=$CONTRACT_ADDRESS
else
    # 从环境变量获取
    CONTRACT_ADDRESS=${CONTRACT_ADDRESS:-"0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab"}
    echo -e "${YELLOW}使用默认合约地址: ${CONTRACT_ADDRESS}${NC}"
fi

# 运行应用
echo -e "${GREEN}启动应用程序...${NC}"
echo -e "${YELLOW}访问地址: http://localhost:8088${NC}"
echo -e "${YELLOW}测试ABI页面: http://localhost:8088/contract-test.html${NC}"
echo -e "${YELLOW}按Ctrl+C终止应用${NC}"

# 使用指定的合约地址启动应用
java -jar -DCONTRACT_ADDRESS=$CONTRACT_ADDRESS target/blockchain-resume-verification-1.0-SNAPSHOT.jar 