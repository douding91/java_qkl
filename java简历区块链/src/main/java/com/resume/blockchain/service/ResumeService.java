package com.resume.blockchain.service;

import com.resume.blockchain.contract.ResumeVerificationContract;
import com.resume.blockchain.entity.Resume;
import java.util.List;

public interface ResumeService {
    Resume createResume(Resume resume);
    Resume getResumeById(Long id);
    List<Resume> getAllResumes();
    Resume updateResume(Long id, Resume resume);
    void deleteResume(Long id);
    Resume verifyResume(Long id, ResumeVerificationContract.ResumeStatus status, String verificationNotes);
} 