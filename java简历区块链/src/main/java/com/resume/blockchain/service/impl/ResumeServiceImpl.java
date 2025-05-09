package com.resume.blockchain.service.impl;

import com.resume.blockchain.contract.ResumeVerificationContract;
import com.resume.blockchain.entity.Resume;
import com.resume.blockchain.repository.ResumeRepository;
import com.resume.blockchain.service.BlockchainService;
import com.resume.blockchain.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.web3j.utils.Numeric;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final BlockchainService blockchainService;
    private static final Logger logger = LoggerFactory.getLogger(ResumeServiceImpl.class);

    @Autowired
    public ResumeServiceImpl(ResumeRepository resumeRepository, BlockchainService blockchainService) {
        this.resumeRepository = resumeRepository;
        this.blockchainService = blockchainService;
    }

    @Override
    @Transactional
    public Resume createResume(Resume resume) {
        try {
            // 先存储到区块链
            blockchainService.storeResume(resume);
            
            // 保存到数据库
            Resume savedResume = resumeRepository.save(resume);
            
            return savedResume;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create resume: " + e.getMessage(), e);
        }
    }

    @Override
    public Resume getResumeById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + id));
    }

    @Override
    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    @Override
    @Transactional
    public Resume updateResume(Long id, Resume resumeDetails) {
        try {
            Resume resume = getResumeById(id);
            logger.info("开始更新简历, ID: {}, 姓名: {}", id, resume.getName());
            
            // 更新数据库中的简历
            resume.setName(resumeDetails.getName());
            resume.setEmail(resumeDetails.getEmail());
            resume.setEducation(resumeDetails.getEducation());
            resume.setWorkExperience(resumeDetails.getWorkExperience());
            resume.setSkills(resumeDetails.getSkills());
            
            // 更新区块链上的简历
            logger.info("更新区块链上的简历信息");
            blockchainService.updateResume(resume);
            logger.info("区块链更新成功");
            
            // 保存到数据库
            Resume updatedResume = resumeRepository.save(resume);
            logger.info("数据库更新成功");
            
            return updatedResume;
        } catch (Exception e) {
            logger.error("更新简历失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update resume: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteResume(Long id) {
        Resume resume = getResumeById(id);
        resumeRepository.delete(resume);
    }

    @Override
    @Transactional
    public Resume verifyResume(Long id, ResumeVerificationContract.ResumeStatus status, String verificationNotes) {
        logger.info("开始验证简历, ID: {}, 状态: {}, 验证说明: {}", id, status, verificationNotes);
        try {
            Resume resume = getResumeById(id);
            logger.info("找到简历, ID: {}, 姓名: {}", id, resume.getName());
            
            // 如果简历还没有存储到区块链，先存储
            if (resume.getBlockchainHash() == null || resume.getBlockchainHash().isEmpty()) {
                logger.info("简历尚未存储到区块链，开始存储...");
                blockchainService.storeResume(resume);
                logger.info("简历已存储到区块链");
            }
            
            // 使用已存储的区块链哈希值进行验证
            String resumeHash = resume.getBlockchainHash();
            logger.info("使用区块链哈希进行验证: {}", resumeHash);
            
            // 验证简历
            blockchainService.verifyResume(resumeHash, status, verificationNotes);
            logger.info("区块链验证成功, 简历哈希: {}", resumeHash);
            
            // 更新简历状态
            resume.setStatus(status.name());
            resume.setVerificationNotes(verificationNotes);
            return resumeRepository.save(resume);
        } catch (Exception e) {
            logger.error("简历验证失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to verify resume: " + e.getMessage(), e);
        }
    }

    private String generateResumeHash(Resume resume) {
        try {
            String content = resume.getName() +
                           resume.getEmail() +
                           resume.getEducation() +
                           resume.getWorkExperience() +
                           resume.getSkills();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Numeric.toHexString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate resume hash: " + e.getMessage(), e);
        }
    }
} 