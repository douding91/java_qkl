package com.resume.blockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.resume.blockchain.entity")
@EnableJpaRepositories("com.resume.blockchain.repository")
public class BlockchainResumeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockchainResumeApplication.class, args);
    }
} 