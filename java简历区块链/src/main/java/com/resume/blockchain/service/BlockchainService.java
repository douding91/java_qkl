package com.resume.blockchain.service;

import com.resume.blockchain.entity.Resume;
import com.resume.blockchain.contract.ResumeVerificationContract;

public interface BlockchainService {
    void storeResume(Resume resume) throws Exception;
    void updateResume(Resume resume) throws Exception;
    void verifyResume(String resumeHash, ResumeVerificationContract.ResumeStatus status, String verificationNotes) throws Exception;
    Resume getResume(String resumeHash) throws Exception;
    void addVerifier(String verifierAddress) throws Exception;
    void removeVerifier(String verifierAddress) throws Exception;
} 