package com.resume.blockchain.service.impl;

import com.resume.blockchain.contract.ResumeVerificationContract;
import com.resume.blockchain.contract.ResumeVerificationContract.ResumeStatus;
import com.resume.blockchain.entity.Resume;
import com.resume.blockchain.service.BlockchainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;
import org.web3j.tuples.generated.Tuple11;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.PostConstruct;

@Service
public class BlockchainServiceImpl implements BlockchainService {
    private static final Logger logger = LoggerFactory.getLogger(BlockchainServiceImpl.class);
    private final Web3j web3j;
    private final Credentials credentials;
    private ResumeVerificationContract contract;
    private BigInteger nonce;

    @Value("${blockchain.contract.owner.private-key}")
    private String privateKey;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Value("${blockchain.network.url}")
    private String networkUrl;

    @Value("${blockchain.network.chainId}")
    private Long chainId;

    @Value("${blockchain.network.gasPrice}")
    private Long gasPrice;

    @Value("${blockchain.network.gasLimit}")
    private Long gasLimit;

    public BlockchainServiceImpl(
            @Value("${blockchain.network.url}") String networkUrl,
            @Value("${blockchain.contract.owner.private-key}") String privateKey,
            @Value("${blockchain.contract.address}") String contractAddress) {
        this.web3j = Web3j.build(new HttpService(networkUrl));
        this.credentials = Credentials.create(privateKey);
        this.nonce = BigInteger.ZERO;
    }

    @PostConstruct
    public void init() throws Exception {
        // 获取当前 nonce
        this.nonce = web3j.ethGetTransactionCount(
            credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount();
        
        logger.info("Current nonce for address {}: {}", credentials.getAddress(), this.nonce);
        logger.info("Using contract address: {}", contractAddress);
        logger.info("Using owner address: {}", credentials.getAddress());

        // 初始化合约
        ContractGasProvider gasProvider = new StaticGasProvider(
            BigInteger.valueOf(gasPrice),
            BigInteger.valueOf(gasLimit)
        );

        // 强制重新部署合约
        logger.info("Deploying new contract...");
        try {
            this.contract = deployNewContract(web3j, credentials, gasProvider);
            logger.info("Contract deployed successfully at address: {}", this.contract.getContractAddress());
            
            // 更新配置文件中的合约地址
            logger.info("Please update your application.yml with the new contract address: {}", this.contract.getContractAddress());
        } catch (Exception e) {
            logger.error("Failed to deploy contract: {}", e.getMessage());
            logger.error("Contract deployment error details:", e);
            throw e;
        }
    }

    /**
     * 部署新合约
     */
    private ResumeVerificationContract deployNewContract(Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) throws Exception {
        logger.info("开始部署新合约...");
        ResumeVerificationContract newContract = ResumeVerificationContract.deploy(
            web3j, 
            credentials, 
            gasProvider
        ).send();
        
        logger.info("Contract deployed successfully at address: {}", newContract.getContractAddress());
        return newContract;
    }

    private synchronized BigInteger getAndIncrementNonce() throws Exception {
        // 从区块链获取最新的 nonce
        BigInteger currentNonce = web3j.ethGetTransactionCount(
            credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount();
        
        // 使用较大的值
        this.nonce = this.nonce.max(currentNonce);
        
        // 记录当前使用的 nonce
        logger.debug("Using nonce: {} for address: {}", this.nonce, credentials.getAddress());
        
        // 返回当前 nonce 并递增
        return this.nonce.add(BigInteger.ONE);
    }

    @Override
    public void storeResume(Resume resume) throws Exception {
        try {
            String resumeHash = generateResumeHash(resume);
            logger.debug("Storing resume with hash: {}", resumeHash);

            // 获取新的 nonce
            BigInteger newNonce = getAndIncrementNonce();
            
            // 设置交易的 nonce
            contract.storeResume(
                resumeHash,
                resume.getName(),
                resume.getEmail(),
                resume.getEducation(),
                resume.getWorkExperience(),
                resume.getSkills(),
                ""  // 空的 IPFS hash，因为我们暂时不使用 IPFS
            ).send();

            // 保存哈希值到简历对象
            resume.setBlockchainHash(resumeHash);
            
            logger.info("Resume stored successfully with hash: {}", resumeHash);
        } catch (Exception e) {
            logger.error("Failed to store resume: {}", e.getMessage(), e);
            throw new Exception("Failed to store resume: " + e.getMessage());
        }
    }

    @Override
    public void updateResume(Resume resume) throws Exception {
        try {
            // 使用原始简历的哈希值
            String originalHash = resume.getBlockchainHash();
            if (originalHash == null || originalHash.isEmpty()) {
                throw new Exception("Resume has no blockchain hash");
            }
            logger.info("更新区块链上的简历, 原始哈希: {}, 调用账户: {}", originalHash, credentials.getAddress());

            // 获取新的 nonce
            BigInteger newNonce = getAndIncrementNonce();
            logger.info("使用 nonce: {}", newNonce);
            
            // 更新区块链上的简历
            logger.info("调用合约更新方法, 参数: name={}, email={}", resume.getName(), resume.getEmail());
            contract.updateResume(
                originalHash,  // 使用原始哈希
                resume.getName(),
                resume.getEmail(),
                resume.getEducation(),
                resume.getWorkExperience(),
                resume.getSkills(),
                ""  // 空的 IPFS hash，因为我们暂时不使用 IPFS
            ).send();
            
            logger.info("区块链简历更新成功, 哈希: {}", originalHash);
        } catch (Exception e) {
            logger.error("更新区块链简历失败: {}, 调用账户: {}", e.getMessage(), credentials.getAddress(), e);
            throw new Exception("Failed to update resume on blockchain: " + e.getMessage());
        }
    }

    @Override
    public void verifyResume(String resumeHash, ResumeVerificationContract.ResumeStatus status, String verificationNotes) throws Exception {
        logger.info("开始区块链验证, 简历哈希: {}, 状态: {}, 验证说明: {}", resumeHash, status, verificationNotes);
        try {
            contract.verifyResume(resumeHash, status, verificationNotes).send();
            logger.info("区块链验证成功, 简历哈希: {}", resumeHash);
        } catch (Exception e) {
            logger.error("区块链验证失败, 简历哈希: {}, 错误: {}", resumeHash, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Resume getResume(String resumeHash) throws Exception {
        Tuple11<String, String, String, String, String, String, BigInteger, String, ResumeStatus, String, BigInteger> result = contract.getResume(resumeHash).send();
        Resume resume = new Resume();
        resume.setName(result.component1());
        resume.setEmail(result.component2());
        resume.setEducation(result.component3());
        resume.setWorkExperience(result.component4());
        resume.setSkills(result.component5());
        resume.setBlockchainHash(resumeHash);
        resume.setStatus(result.component9().name());
        resume.setVerificationNotes(result.component10());
        return resume;
    }

    @Override
    public void addVerifier(String verifierAddress) throws Exception {
        contract.addVerifier(verifierAddress).send();
    }

    @Override
    public void removeVerifier(String verifierAddress) throws Exception {
        contract.removeVerifier(verifierAddress).send();
    }

    private String generateResumeHash(Resume resume) throws NoSuchAlgorithmException {
        // 将简历信息拼接成字符串
        String content = String.join("|",
            resume.getName(),
            resume.getEmail(),
            resume.getEducation(),
            resume.getWorkExperience(),
            resume.getSkills()
        );

        // 使用 SHA-256 生成哈希
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        
        // 转换为十六进制字符串
        return Numeric.toHexString(hash);
    }
} 