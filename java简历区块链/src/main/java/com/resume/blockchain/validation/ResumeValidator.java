package com.resume.blockchain.validation;

import com.resume.blockchain.entity.Resume;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import java.util.regex.Pattern;

@Component
public class ResumeValidator implements Validator {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_EDUCATION_LENGTH = 1000;
    private static final int MAX_WORK_EXPERIENCE_LENGTH = 2000;
    private static final int MAX_SKILLS_LENGTH = 1000;

    @Override
    public boolean supports(Class<?> clazz) {
        return Resume.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Resume resume = (Resume) target;

        // 验证姓名
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "field.required", "姓名不能为空");
        if (resume.getName() != null && resume.getName().length() > MAX_NAME_LENGTH) {
            errors.rejectValue("name", "field.length", "姓名长度不能超过" + MAX_NAME_LENGTH + "个字符");
        }

        // 验证邮箱
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "field.required", "邮箱不能为空");
        if (resume.getEmail() != null) {
            if (resume.getEmail().length() > MAX_EMAIL_LENGTH) {
                errors.rejectValue("email", "field.length", "邮箱长度不能超过" + MAX_EMAIL_LENGTH + "个字符");
            }
            if (!Pattern.matches(EMAIL_PATTERN, resume.getEmail())) {
                errors.rejectValue("email", "field.invalid", "邮箱格式不正确");
            }
        }

        // 验证教育经历
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "education", "field.required", "教育经历不能为空");
        if (resume.getEducation() != null && resume.getEducation().length() > MAX_EDUCATION_LENGTH) {
            errors.rejectValue("education", "field.length", "教育经历长度不能超过" + MAX_EDUCATION_LENGTH + "个字符");
        }

        // 验证工作经历
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "workExperience", "field.required", "工作经历不能为空");
        if (resume.getWorkExperience() != null && resume.getWorkExperience().length() > MAX_WORK_EXPERIENCE_LENGTH) {
            errors.rejectValue("workExperience", "field.length", "工作经历长度不能超过" + MAX_WORK_EXPERIENCE_LENGTH + "个字符");
        }

        // 验证技能
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "skills", "field.required", "技能不能为空");
        if (resume.getSkills() != null && resume.getSkills().length() > MAX_SKILLS_LENGTH) {
            errors.rejectValue("skills", "field.length", "技能长度不能超过" + MAX_SKILLS_LENGTH + "个字符");
        }
    }
} 