package com.resume.blockchain.contract;

import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.protocol.Web3j;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.abi.datatypes.generated.Uint8;
import java.math.BigInteger;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.Address;
import org.web3j.tuples.generated.Tuple11;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.exceptions.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class ResumeVerificationContract extends Contract {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeVerificationContract.class);
    
    public enum ResumeStatus {
        PENDING(0),
        VERIFIED(1),
        REJECTED(2);
        
        private final int value;
        
        ResumeStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static ResumeStatus fromValue(int value) {
            for (ResumeStatus status : ResumeStatus.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status value: " + value);
        }
    }

    /**
     * web3j 部署合约使用的构造方法
     */
    public ResumeVerificationContract(String contractBinary, String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        super(contractBinary, contractAddress, web3j, credentials, gasProvider);
    }

    /**
     * web3j 部署合约时内部调用的构造方法
     */
    public ResumeVerificationContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        super("", contractAddress, web3j, credentials, gasProvider);
    }

    public static ResumeVerificationContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        return new ResumeVerificationContract("", contractAddress, web3j, credentials, gasProvider);
    }
    
    /**
     * 部署合约
     * 
     * @param web3j web3j实例
     * @param credentials 凭证
     * @param gasProvider gas提供者
     * @return RemoteCall<ResumeVerificationContract> 返回可发送的远程函数调用
     */
    public static RemoteCall<ResumeVerificationContract> deploy(
            Web3j web3j,
            Credentials credentials,
            ContractGasProvider gasProvider) {
        
        try {
            // 读取合约二进制代码
            String contractBinary = readContractBinary();
            logger.info("Deploying contract with binary of size: {}", contractBinary.length());
            
            // 创建部署交易
            return deployRemoteCall(
                ResumeVerificationContract.class,
                web3j, 
                credentials, 
                gasProvider,
                contractBinary, 
                ""  // 构造函数参数，这里是空字符串，因为合约构造函数没有参数
            );
        } catch (Exception e) {
            logger.error("Error preparing contract deployment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to prepare contract deployment: " + e.getMessage(), e);
        }
    }
    
    /**
     * 读取合约二进制代码
     * 
     * @return 合约二进制代码
     * @throws IOException 如果读取失败
     */
    private static String readContractBinary() throws IOException {
        try {
            // 读取合约二进制文件
            String contractJson = new String(Files.readAllBytes(
                Paths.get("build/contracts/ResumeVerification.json")));
            
            // 使用更可靠的方式提取 bytecode
            // 正则表达式匹配 "bytecode": "0x...", 避免简单字符串搜索可能的问题
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"bytecode\":\\s*\"(0x[0-9a-fA-F]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(contractJson);
            
            if (matcher.find()) {
                String bytecode = matcher.group(1);
                if (bytecode == null || bytecode.isEmpty()) {
                    throw new IOException("Contract bytecode is empty");
                }
                
                logger.info("Read contract binary: bytecode length = {}, starting with: {}", 
                           bytecode.length(), 
                           bytecode.substring(0, Math.min(10, bytecode.length())));
                
                return bytecode;
            } else {
                throw new IOException("Could not find bytecode in contract JSON");
            }
        } catch (Exception e) {
            logger.error("Error reading contract binary: {}", e.getMessage(), e);
            // 如果我们无法从文件读取字节码，将抛出异常
            throw new IOException("Failed to read contract binary: " + e.getMessage(), e);
        }
    }

    public RemoteFunctionCall<TransactionReceipt> storeResume(
            String resumeHash,
            String name,
            String email,
            String education,
            String workExperience,
            String skills,
            String ipfsHash
    ) {
        final Function function = new Function(
                "storeResume",
                Arrays.asList(
                        new Utf8String(resumeHash),
                        new Utf8String(name),
                        new Utf8String(email),
                        new Utf8String(education),
                        new Utf8String(workExperience),
                        new Utf8String(skills),
                        new Utf8String(ipfsHash)
                ),
                Arrays.asList()
        );
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateResume(
            String resumeHash,
            String name,
            String email,
            String education,
            String workExperience,
            String skills,
            String ipfsHash
    ) {
        final Function function = new Function(
                "updateResume",
                Arrays.asList(
                        new Utf8String(resumeHash),
                        new Utf8String(name),
                        new Utf8String(email),
                        new Utf8String(education),
                        new Utf8String(workExperience),
                        new Utf8String(skills),
                        new Utf8String(ipfsHash)
                ),
                Arrays.asList()
        );
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> verifyResume(
            String resumeHash,
            ResumeStatus status,
            String verificationNotes
    ) {
        final Function function = new Function(
                "verifyResume",
                Arrays.asList(
                        new Utf8String(resumeHash),
                        new Uint8(BigInteger.valueOf(status.getValue())),
                        new Utf8String(verificationNotes)
                ),
                Arrays.asList()
        );
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple11<String, String, String, String, String, String, BigInteger, String, ResumeStatus, String, BigInteger>> getResume(String resumeHash) {
        final Function function = new Function(
                "getResume",
                Arrays.asList(new Utf8String(resumeHash)),
                Arrays.asList(
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Uint8>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint8>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Uint8>() {}
                )
        );
        return new RemoteFunctionCall<>(
                function,
                new Callable<Tuple11<String, String, String, String, String, String, BigInteger, String, ResumeStatus, String, BigInteger>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Tuple11<String, String, String, String, String, String, BigInteger, String, ResumeStatus, String, BigInteger> call() throws Exception {
                        List<Type> result = executeCallSingleValueReturn(function);
                        return new Tuple11<>(
                                (String) result.get(0).getValue(),
                                (String) result.get(1).getValue(),
                                (String) result.get(2).getValue(),
                                (String) result.get(3).getValue(),
                                (String) result.get(4).getValue(),
                                (String) result.get(5).getValue(),
                                (BigInteger) result.get(6).getValue(),
                                ((Address) result.get(7)).getValue(),
                                ResumeStatus.fromValue(((Uint8) result.get(8)).getValue().intValue()),
                                (String) result.get(9).getValue(),
                                (BigInteger) result.get(10).getValue()
                        );
                    }
                }
        );
    }

    public RemoteFunctionCall<TransactionReceipt> addVerifier(String verifierAddress) {
        final Function function = new Function(
                "addVerifier",
                Arrays.asList(new Address(verifierAddress)),
                Arrays.asList()
        );
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeVerifier(String verifierAddress) {
        final Function function = new Function(
                "removeVerifier",
                Arrays.asList(new Address(verifierAddress)),
                Arrays.asList()
        );
        return executeRemoteCallTransaction(function);
    }
} 