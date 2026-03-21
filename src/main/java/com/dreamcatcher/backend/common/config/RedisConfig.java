package com.dreamcatcher.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    /**
     * 1. Redis와 연결을 맺어주는 공장(Factory)을 만듭니다.
     * 실무에서는 성능이 좋은 Lettuce를 기본으로 사용합니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 1. 기본 설정 객체 생성 (Host, Port)
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);

        // 2. 비밀번호 설정 (중요!)
        // application.yml 등에 설정된 비밀번호 변수를 사용하거나 직접 문자열 입력
        config.setPassword(password);

        // 3. 설정 정보를 담아 Factory 생성
        return new LettuceConnectionFactory(config);
    }

    /**
     * 2. 우리가 실제로 코딩할 때 Redis에 명령을 내릴 템플릿(도구)입니다.
     * Key와 Value를 어떤 형식으로 저장할지 '직렬화(Serializer)' 설정을 해줍니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // 연결 공장 세팅
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // Key는 보통 String으로 저장하므로 String 직렬화 설정
        // 우리가 레디스 환경에서 눈으로 읽을 수 있는 문자로 저장하기 위함
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // Value도 일단 보기 편하게 String 직렬화로 설정 (나중에 JSON 객체를 저장할 때는 Jackson 직렬화기로 변경 가능)
        // 우리가 레디스 환경에서 눈으로 읽을 수 있는 문자로 저장하기 위함
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        // Hash 자료구조를 쓸 때를 위한 설정
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}