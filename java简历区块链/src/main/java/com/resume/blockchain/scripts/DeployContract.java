package com.resume.blockchain.scripts;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import java.math.BigInteger;

public class DeployContract {
    public static void main(String[] args) {
        try {
            // Connect to Ganache
            Web3j web3j = Web3j.build(new HttpService("http://localhost:7545"));
            
            // Load credentials
            String privateKey = "4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d";
            Credentials credentials = Credentials.create(privateKey);
            
            // Set gas parameters
            ContractGasProvider gasProvider = new StaticGasProvider(
                BigInteger.valueOf(20000000000L), // 20 Gwei
                BigInteger.valueOf(4_300_000L)    // Gas Limit
            );
            
            // Deploy contract
            System.out.println("Deploying contract...");
            String contractAddress = ""; // TODO: Deploy contract and get address
            
            System.out.println("Contract deployed at: " + contractAddress);
            
        } catch (Exception e) {
            System.err.println("Error deploying contract: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 