# 区块链简历验证系统

基于区块链技术的简历验证系统，使用 Spring Boot 和以太坊智能合约实现。该系统允许用户创建、存储和验证简历，所有操作都会被记录在区块链上，确保数据的不可篡改性和可追溯性。

## 技术栈

- 后端：Spring Boot 2.7.0
- 数据库：H2 Database
- 区块链：以太坊（使用 Ganache 作为本地测试网络）
- 智能合约：Solidity 0.8.0
- 前端：Thymeleaf + Bootstrap

## 系统要求

- Java 8 或更高版本
- Node.js 20.x
- Maven 3.6 或更高版本
- Ganache CLI

## 项目结构

```
blockchain-resume-verification/
├── contracts/                    # 智能合约目录
│   └── ResumeVerification.sol    # 简历验证智能合约
├── migrations/                   # Truffle 迁移文件
│   └── 1_deploy_contracts.js     # 合约部署脚本
├── src/
│   └── main/
│       ├── java/
│       │   └── com/resume/blockchain/
│       │       ├── config/       # Spring 配置
│       │       ├── controller/   # REST 控制器
│       │       ├── entity/       # 数据实体
│       │       ├── repository/   # 数据访问层
│       │       ├── service/      # 业务逻辑层
│       │       └── BlockchainResumeApplication.java
│       └── resources/
│           ├── static/          # 静态资源
│           ├── templates/       # Thymeleaf 模板
│           └── application.yml  # 应用配置
└── pom.xml                      # Maven 配置
```

## 安装和配置

### 1. 安装依赖

```bash
# 安装 Node.js 依赖
npm install

# 安装 Maven 依赖
mvn install
```

### 2. 配置区块链环境

#### 2.1 安装 Ganache

```bash
# 安装 Ganache CLI
npm install -g ganache
```

#### 2.2 启动本地区块链网络

```bash
# 启动 Ganache，使用指定的账户和余额
ganache --account="0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d,100000000000000000000" --port 8545
```

#### 2.3 部署智能合约

```bash
# 编译合约
npx truffle compile

# 部署合约
npx truffle migrate --reset
```

部署完成后，会输出合约地址。需要将这个地址更新到 `application.yml` 文件中：

```yaml
blockchain:
  network:
    url: http://localhost:8545
  contract:
    address: "你的合约地址"
  wallet:
    private-key: "4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d"
```

### 3. 启动应用

```bash
# 启动 Spring Boot 应用
mvn spring-boot:run
```

应用将在 http://localhost:8080 启动。

## 智能合约说明

### ResumeVerification.sol

该智能合约实现了以下主要功能：

1. 简历存储
   - 存储简历的基本信息（姓名、邮箱、教育经历等）
   - 生成唯一的简历哈希值
   - 记录简历的所有者和时间戳

2. 简历验证
   - 验证者可以验证简历的真实性
   - 支持三种验证状态：待验证、已验证、已拒绝
   - 记录验证备注和时间戳

3. 权限管理
   - 只有验证者可以验证简历
   - 只有简历所有者可以更新简历
   - 支持添加和移除验证者

### 合约接口

```solidity
// 存储简历
function storeResume(
    string memory _resumeHash,
    string memory _name,
    string memory _email,
    string memory _education,
    string memory _workExperience,
    string memory _skills
) external

// 更新简历
function updateResume(
    string memory _resumeHash,
    string memory _name,
    string memory _email,
    string memory _education,
    string memory _workExperience,
    string memory _skills
) external

// 验证简历
function verifyResume(
    string memory _resumeHash,
    ResumeStatus _status,
    string memory _notes
) external

// 获取简历信息
function getResume(string memory _resumeHash) external view returns (
    string memory name,
    string memory email,
    string memory education,
    string memory workExperience,
    string memory skills,
    string memory ipfsHash,
    uint256 timestamp,
    address owner,
    ResumeStatus status,
    string memory verificationNotes,
    uint256 lastUpdated
)
```

## API 接口

### 简历管理

- POST /api/resumes - 创建新简历
- GET /api/resumes/{id} - 获取简历详情
- GET /api/resumes - 获取所有简历
- PUT /api/resumes/{id} - 更新简历
- DELETE /api/resumes/{id} - 删除简历
- POST /api/resumes/{id}/verify - 验证简历

## 注意事项

1. 确保 Ganache 在运行状态，且账户有足够的 ETH
2. 合约部署后，需要更新 application.yml 中的合约地址
3. 私钥配置中不要包含 "0x" 前缀
4. 确保 8545 端口未被占用

## 故障排除

1. "sender doesn't have enough funds" 错误
   - 检查 Ganache 是否正在运行
   - 确认账户余额是否充足
   - 验证 gas 价格设置是否合理

2. 合约部署失败
   - 检查 Solidity 编译器版本
   - 确保 Ganache 网络正常运行
   - 验证账户权限和余额

3. 应用启动失败
   - 检查配置文件中的合约地址是否正确
   - 确认区块链网络连接是否正常
   - 查看应用日志获取详细错误信息

## 开发计划

- [ ] 添加 IPFS 支持，存储简历详细内容
- [ ] 实现多链支持
- [ ] 添加简历模板功能
- [ ] 优化 gas 消耗
- [ ] 添加批量验证功能

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License 