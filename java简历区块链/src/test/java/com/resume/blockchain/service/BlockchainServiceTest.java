package com.resume.blockchain.service;

import com.resume.blockchain.contract.ResumeVerificationContract;
import com.resume.blockchain.entity.Resume;
import com.resume.blockchain.service.impl.BlockchainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BlockchainServiceTest {

    @Mock
    private ResumeVerificationContract contract;

    @Mock
    private Web3j web3j;

    private BlockchainServiceImpl blockchainService;

    private static final String TEST_NETWORK_URL = "http://localhost:7545";
    private static final String TEST_PRIVATE_KEY = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    private static final String TEST_CONTRACT_ADDRESS = "0x1234567890123456789012345678901234567890";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        blockchainService = new BlockchainServiceImpl(
            TEST_NETWORK_URL,
            TEST_PRIVATE_KEY,
            TEST_CONTRACT_ADDRESS
        );
        
        // 设置 credentials
        Credentials credentials = Credentials.create(TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(blockchainService, "credentials", credentials);
        
        // 设置其他字段
        ReflectionTestUtils.setField(blockchainService, "contract", contract);
        ReflectionTestUtils.setField(blockchainService, "web3j", web3j);
        ReflectionTestUtils.setField(blockchainService, "networkUrl", TEST_NETWORK_URL);
        ReflectionTestUtils.setField(blockchainService, "privateKey", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(blockchainService, "contractAddress", TEST_CONTRACT_ADDRESS);
        ReflectionTestUtils.setField(blockchainService, "chainId", 1337L);
        ReflectionTestUtils.setField(blockchainService, "gasPrice", 20000000000L);
        ReflectionTestUtils.setField(blockchainService, "gasLimit", 6721975L);

        // 模拟 web3j.ethGetTransactionCount 的返回值
        EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
        when(ethGetTransactionCount.getTransactionCount()).thenReturn(BigInteger.ZERO);
        
        // 修复这里的模拟代码
        org.web3j.protocol.core.Request<?, EthGetTransactionCount> requestMock = 
            (org.web3j.protocol.core.Request<?, EthGetTransactionCount>) mock(org.web3j.protocol.core.Request.class);
        when(requestMock.send()).thenReturn(ethGetTransactionCount);
        
        when(web3j.ethGetTransactionCount(any(), eq(DefaultBlockParameterName.LATEST)))
            .thenReturn(requestMock);
    }

    @Test
    void testStoreResume() throws Exception {
        // 准备测试数据
        Resume resume = new Resume();
        resume.setName("Test Name");
        resume.setEmail("test@example.com");
        resume.setEducation("Test Education");
        resume.setWorkExperience("Test Experience");
        resume.setSkills("Test Skills");
        resume.setIpfsHash("QmTest123");

        // 模拟合约调用
        RemoteFunctionCall<TransactionReceipt> mockCall = mock(RemoteFunctionCall.class);
        when(contract.storeResume(
            any(), // resumeHash
            any(), // name
            any(), // email
            any(), // education
            any(), // workExperience
            any(), // skills
            any()  // ipfsHash
        )).thenReturn(mockCall);
        when(mockCall.send()).thenReturn(mock(TransactionReceipt.class));

        // 执行测试
        assertDoesNotThrow(() -> blockchainService.storeResume(resume));

        // 验证合约调用
        verify(contract, times(1)).storeResume(
            any(), // resumeHash
            eq(resume.getName()),
            eq(resume.getEmail()),
            eq(resume.getEducation()),
            eq(resume.getWorkExperience()),
            eq(resume.getSkills()),
            eq(resume.getIpfsHash())
        );
    }

    @Test
    void testVerifyResume() throws Exception {
        // 准备测试数据
        String resumeHash = "0x1234567890abcdef";
        ResumeVerificationContract.ResumeStatus status = ResumeVerificationContract.ResumeStatus.VERIFIED;
        String verificationNotes = "Verified successfully";

        // 模拟合约调用
        RemoteFunctionCall<TransactionReceipt> mockCall = mock(RemoteFunctionCall.class);
        when(contract.verifyResume(any(), any(), any())).thenReturn(mockCall);
        when(mockCall.send()).thenReturn(mock(TransactionReceipt.class));

        // 执行测试
        assertDoesNotThrow(() -> blockchainService.verifyResume(resumeHash, status, verificationNotes));

        // 验证合约调用
        verify(contract, times(1)).verifyResume(
            eq(resumeHash),
            eq(status),
            eq(verificationNotes)
        );
    }
} 