package com.achilles.chat_service.sensitive;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SensitiveWordConfig {
    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        return SensitiveWordBs.newInstance()
                // 使用默认白名单+自定义白名单
                .wordAllow(WordAllows.chains(WordAllows.defaults(), new CustomWordAllow()))
                // 使用默认黑名单+自定义黑名单
                .wordDeny(WordDenys.chains(WordDenys.defaults(), new CustomWordDeny()))
                // 忽略大小写
                .ignoreCase( true)
                // 忽略全角半角
                .ignoreWidth( true)
                // 忽略数字风格
                .ignoreNumStyle( true)
                // 忽略中文风格
                .ignoreChineseStyle( true)
                // 忽略英文风格
                .ignoreEnglishStyle( true)
                .init();
    }
}
