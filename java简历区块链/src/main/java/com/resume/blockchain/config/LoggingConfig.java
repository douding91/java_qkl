package com.resume.blockchain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

@Configuration
public class LoggingConfig {

    @Autowired
    private Environment env;

    @Bean
    @Primary
    public LoggerContext loggerContext() {
        LoggerContext loggerContext = new LoggerContext();
        
        // 控制台输出
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("CONSOLE");
        
        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(loggerContext);
        consoleEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        consoleEncoder.start();
        
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();
        
        // 文件输出
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("FILE");
        fileAppender.setFile("logs/blockchain-resume.log");
        
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern("logs/blockchain-resume.%d{yyyy-MM-dd}.%i.log");
        rollingPolicy.setMaxFileSize(new FileSize(10 * 1024 * 1024)); // 10MB
        rollingPolicy.setMaxHistory(30);
        rollingPolicy.start();
        
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        fileEncoder.start();
        
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();
        
        // 配置根日志记录器
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(consoleAppender);
        rootLogger.addAppender(fileAppender);
        
        // 配置应用日志记录器
        Logger appLogger = loggerContext.getLogger("com.resume.blockchain");
        appLogger.setLevel(Level.DEBUG);
        appLogger.setAdditive(false);
        appLogger.addAppender(consoleAppender);
        appLogger.addAppender(fileAppender);
        
        return loggerContext;
    }
} 