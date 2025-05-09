let web3;
let contract;
let userAccount;

// 我们将从API获取这些值
let contractAddress = '';
let contractABI = [];
let initAttempts = 0;
const MAX_INIT_ATTEMPTS = 3;

// 初始化函数
async function initializeContract() {
    if (initAttempts >= MAX_INIT_ATTEMPTS) {
        console.error('超过最大初始化尝试次数');
        showAlert('danger', '合约初始化失败，请刷新页面重试');
        return false;
    }
    
    initAttempts++;
    console.log(`尝试初始化合约 (${initAttempts}/${MAX_INIT_ATTEMPTS})...`);
    
    try {
        // 从API获取合约信息
        console.log('正在获取合约信息...');
        const contractInfoResponse = await fetch('/api/contract/info');
        if (!contractInfoResponse.ok) {
            const errorData = await contractInfoResponse.json().catch(() => ({}));
            throw new Error(`获取合约信息失败: ${errorData.error || contractInfoResponse.statusText}`);
        }
        
        const contractInfo = await contractInfoResponse.json();
        contractAddress = contractInfo.address;
        console.log('获取到合约地址:', contractAddress);
        
        // 解析ABI
        try {
            // 确保ABI是有效的JSON格式
            if (typeof contractInfo.abi === 'string') {
                console.log('解析ABI字符串...');
                // 清理字符串，去除多余空格和换行符
                const cleanAbiString = contractInfo.abi.trim().replace(/\n/g, '').replace(/\s+/g, ' ');
                console.log('清理后的ABI字符串长度:', cleanAbiString.length);
                
                try {
                    contractABI = JSON.parse(cleanAbiString);
                } catch (innerError) {
                    console.error('尝试解析清理后的ABI失败:', innerError);
                    // 尝试修复常见的格式问题
                    if (cleanAbiString.startsWith(' [')) {
                        // 移除前导空格
                        const fixedAbi = cleanAbiString.trim();
                        console.log('尝试修复ABI格式，移除前导空格');
                        contractABI = JSON.parse(fixedAbi);
                    } else {
                        throw innerError;
                    }
                }
            } else if (Array.isArray(contractInfo.abi)) {
                // 如果已经是数组，直接使用
                contractABI = contractInfo.abi;
            } else {
                throw new Error('ABI格式错误');
            }
            console.log('成功加载合约ABI，长度为:', contractABI.length);
            
            // 验证ABI
            if (!contractABI || contractABI.length === 0) {
                throw new Error('ABI为空数组');
            }
            
            // 添加ABI验证，确保至少包含必要的方法
            const requiredMethods = ['storeResume', 'getResume', 'getUserResumes'];
            const missingMethods = requiredMethods.filter(method => 
                !contractABI.some(item => item.name === method && item.type === 'function')
            );
            
            if (missingMethods.length > 0) {
                console.warn('ABI缺少必要的方法:', missingMethods);
                showAlert('warning', `ABI缺少必要的方法: ${missingMethods.join(', ')}`);
            }
        } catch (parseError) {
            console.error('解析合约ABI失败:', parseError);
            console.error('ABI内容:', contractInfo.abi);
            showAlert('danger', `ABI解析失败: ${parseError.message}`);
            
            // 延迟后重试初始化
            setTimeout(() => initializeContract(), 3000);
            return false;
        }
        
        // 初始化Web3
        if (typeof window.ethereum !== 'undefined') {
            console.log('检测到MetaMask，初始化Web3...');
            web3 = new Web3(window.ethereum);
            try {
                await window.ethereum.request({ method: 'eth_requestAccounts' });
                userAccount = (await web3.eth.getAccounts())[0];
                
                // 确保合约ABI和地址都有效
                if (!contractABI || contractABI.length === 0) {
                    throw new Error('合约ABI无效');
                }
                if (!contractAddress || contractAddress === '0x0000000000000000000000000000000000000000') {
                    throw new Error('合约地址无效');
                }
                
                console.log('初始化合约实例...');
                contract = new web3.eth.Contract(contractABI, contractAddress);
                
                // 验证合约实例
                if (!contract || !contract.methods) {
                    throw new Error('合约实例初始化失败');
                }
                
                console.log('Web3初始化成功，连接账户:', userAccount);
                showAlert('success', '区块链连接成功');
                return true;
            } catch (error) {
                console.error('Web3初始化失败:', error);
                showAlert('warning', `Web3初始化失败: ${error.message}`);
                
                // 延迟后重试初始化
                setTimeout(() => initializeContract(), 3000);
                return false;
            }
        } else {
            console.error('未检测到MetaMask');
            showAlert('warning', '请安装MetaMask浏览器插件');
            return false;
        }
    } catch (error) {
        console.error('初始化失败:', error);
        showAlert('danger', `初始化失败: ${error.message}`);
        
        // 延迟后重试初始化
        setTimeout(() => initializeContract(), 3000);
        return false;
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    // 尝试初始化合约
    const success = await initializeContract();
    
    if (!success) {
        console.warn('初始化合约失败，页面功能可能受限');
    }

    // 初始化表单提交事件监听
    const resumeForm = document.getElementById('resumeForm');
    if (resumeForm) {
        resumeForm.addEventListener('submit', handleResumeSubmit);
    }

    // 初始化导航事件监听
    document.getElementById('createResume').addEventListener('click', showCreateResume);
    document.getElementById('viewResumes').addEventListener('click', showResumeList);
    document.getElementById('verifyResume').addEventListener('click', showVerifyResume);
});

function showCreateResume() {
    document.getElementById('createResumeForm').style.display = 'block';
    document.getElementById('resumeList').style.display = 'none';
    document.getElementById('verifyResumeForm').style.display = 'none';
}

function showResumeList() {
    document.getElementById('createResumeForm').style.display = 'none';
    document.getElementById('resumeList').style.display = 'block';
    document.getElementById('verifyResumeForm').style.display = 'none';
    loadResumes();
}

function showVerifyResume() {
    document.getElementById('createResumeForm').style.display = 'none';
    document.getElementById('resumeList').style.display = 'none';
    document.getElementById('verifyResumeForm').style.display = 'block';
}

async function handleResumeSubmit(event) {
    event.preventDefault();
    console.log('Form submitted'); // 调试日志
    
    const resume = {
        name: document.getElementById('name').value,
        email: document.getElementById('email').value,
        education: document.getElementById('education').value,
        workExperience: document.getElementById('workExperience').value,
        skills: document.getElementById('skills').value,
        phone: document.getElementById('phone').value || '' // 添加电话号码字段
    };

    try {
        console.log('Submitting resume:', resume); // 调试日志

        const response = await fetch('/api/resumes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(resume)
        });

        console.log('Response status:', response.status); // 调试日志

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            console.error('Error response:', errorData); // 调试日志
            throw new Error(errorData?.message || '简历创建失败');
        }

        const result = await response.json();
        console.log('Success response:', result); // 调试日志

        showAlert('success', '简历创建成功！');
        document.getElementById('resumeForm').reset();
        showResumeList();
    } catch (error) {
        console.error('Error creating resume:', error);
        showAlert('danger', '简历创建失败：' + error.message);
    }
}

async function handleVerifySubmit(event) {
    event.preventDefault();
    
    const resumeHash = document.getElementById('resumeHash').value;

    try {
        // 调用智能合约验证简历
        await contract.methods.verifyResume(resumeHash).send({ from: userAccount });
        showAlert('success', '简历验证成功！');
    } catch (error) {
        console.error('Error verifying resume:', error);
        showAlert('danger', '简历验证失败：' + error.message);
    }
}

async function loadResumes() {
    try {
        if (!contract || !contract.methods) {
            showAlert('warning', '合约未初始化，请刷新页面重试');
            // 如果合约未初始化，尝试重新初始化
            initializeContract();
            return;
        }
        
        // 获取用户的简历列表
        const resumes = await contract.methods.getUserResumes(userAccount).call();
        const resumeListContent = document.getElementById('resumeListContent');
        resumeListContent.innerHTML = '';

        if (resumes.length === 0) {
            resumeListContent.innerHTML = '<div class="alert alert-info">您还没有创建任何简历</div>';
            return;
        }

        for (const resumeHash of resumes) {
            try {
                const resume = await contract.methods.getResume(resumeHash).call();
                const resumeElement = createResumeElement(resume, resumeHash);
                resumeListContent.appendChild(resumeElement);
            } catch (error) {
                console.error(`获取简历 ${resumeHash} 失败:`, error);
                // 继续处理其他简历
            }
        }
    } catch (error) {
        console.error('加载简历失败:', error);
        showAlert('danger', '加载简历失败：' + error.message);
    }
}

function createResumeElement(resume, hash) {
    const div = document.createElement('div');
    div.className = 'resume-item';
    div.innerHTML = `
        <h4>${resume.name}</h4>
        <p><strong>邮箱：</strong>${resume.email}</p>
        <p><strong>教育经历：</strong>${resume.education}</p>
        <p><strong>工作经历：</strong>${resume.workExperience}</p>
        <p><strong>技能：</strong>${resume.skills}</p>
        <p><strong>哈希值：</strong>${hash}</p>
        <p><strong>创建时间：</strong>${new Date(resume.timestamp * 1000).toLocaleString()}</p>
    `;
    return div;
}

function showAlert(type, message) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.querySelector('.container').appendChild(alertDiv);
    setTimeout(() => alertDiv.remove(), 5000);
}

document.getElementById('verifyForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const resumeId = document.getElementById('resumeId').value;
    const status = document.getElementById('verificationStatus').value;
    const notes = document.getElementById('verificationNotes').value;
    
    try {
        const response = await fetch(`/api/resumes/${resumeId}/verify`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `status=${encodeURIComponent(status)}&verificationNotes=${encodeURIComponent(notes)}`
        });
        
        if (response.ok) {
            showAlert('success', '简历验证成功！');
            document.getElementById('verifyForm').reset();
        } else {
            const error = await response.text();
            showAlert('danger', '验证失败：' + error);
        }
    } catch (error) {
        showAlert('danger', '验证失败：' + error.message);
    }
}); 