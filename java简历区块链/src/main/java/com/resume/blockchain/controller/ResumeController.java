package com.resume.blockchain.controller;

import com.resume.blockchain.contract.ResumeVerificationContract;
import com.resume.blockchain.entity.Resume;
import com.resume.blockchain.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "*") // 允许跨域请求
public class ResumeController {

    private final ResumeService resumeService;

    @Autowired
    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<?> createResume(@RequestBody Resume resume) {
        try {
            Resume createdResume = resumeService.createResume(resume);
            return ResponseEntity.ok(createdResume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResumeById(@PathVariable Long id) {
        try {
            Resume resume = resumeService.getResumeById(id);
            return ResponseEntity.ok(resume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllResumes() {
        try {
            List<Resume> resumes = resumeService.getAllResumes();
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResume(@PathVariable Long id, @RequestBody Resume resume) {
        try {
            Resume updatedResume = resumeService.updateResume(id, resume);
            return ResponseEntity.ok(updatedResume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable Long id) {
        try {
            resumeService.deleteResume(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyResume(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String verificationNotes) {
        try {
            // 将字符串状态转换为枚举
            ResumeVerificationContract.ResumeStatus resumeStatus;
            try {
                int statusValue = Integer.parseInt(status);
                resumeStatus = ResumeVerificationContract.ResumeStatus.fromValue(statusValue);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "无效的状态值，请输入数字：0(待验证)、1(已验证)或2(已拒绝)"));
            }
            
            Resume verifiedResume = resumeService.verifyResume(id, resumeStatus, verificationNotes);
            return ResponseEntity.ok(verifiedResume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
} 