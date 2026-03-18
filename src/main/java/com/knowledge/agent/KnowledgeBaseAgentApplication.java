package com.knowledge.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.knowledge.agent.config.AiProperties;
import com.knowledge.agent.config.MilvusProperties;
import com.knowledge.agent.config.RagProperties;
import com.knowledge.agent.config.StorageProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.knowledge.agent.mapper")
@EnableConfigurationProperties({AiProperties.class, MilvusProperties.class, RagProperties.class, StorageProperties.class})
public class KnowledgeBaseAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseAgentApplication.class, args);
    }

    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.ASSIGN_ID);
        globalConfig.setDbConfig(dbConfig);
        return globalConfig;
    }
}
