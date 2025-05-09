package com.resume.blockchain.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contract")
@CrossOrigin(origins = "*") // 允许跨域请求
public class ContractController {

    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getContractInfo() {
        try {
            // 读取合约 ABI
            String contractJson = new String(Files.readAllBytes(
                    Paths.get("build/contracts/ResumeVerification.json")));

            // 使用Jackson解析JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(contractJson);
            JsonNode abiNode = root.get("abi");
            
            // 返回合约信息
            Map<String, Object> contractInfo = new HashMap<>();
            contractInfo.put("address", contractAddress);
            
            // 直接将ABI节点作为Object添加，这样Jackson会将其序列化为JSON数组
            contractInfo.put("abi", abiNode);
            
            logger.info("Contract info retrieved successfully. Address: {}, ABI size: {}", 
                       contractAddress, abiNode.size());
            return ResponseEntity.ok(contractInfo);
        } catch (Exception e) {
            logger.error("Error retrieving contract info: {}", e.getMessage(), e);
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", "Failed to retrieve contract info: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorInfo);
        }
    }
} 