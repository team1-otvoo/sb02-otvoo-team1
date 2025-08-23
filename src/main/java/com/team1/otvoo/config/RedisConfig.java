package com.team1.otvoo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team1.otvoo.sse.event.RedisStreamListener;
import com.team1.otvoo.sse.event.RedisSubscriber;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

@Configuration
public class RedisConfig {

  private static final String STREAM_KEY = "sse-stream";

  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }

  // Stream 리스너 설정
  @Bean
  public Subscription streamMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisStreamListener streamListener
  ) {
    StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions
            .<String, ObjectRecord<String, String>>builder()
            .pollTimeout(Duration.ofSeconds(1))
            .targetType(String.class) // 리스너가 String 값을 기대하도록
            .build();

    StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
        StreamMessageListenerContainer.create(connectionFactory, options);

    // Consumer Group을 사용하지 않고, 모든 인스턴스가 모든 메시지를 받도록 설정
    // StreamOffset.latest() : 리스너 시작 시점 이후의 메시지만 받음
    Subscription subscription = container.receive(
        StreamOffset.latest(STREAM_KEY),
        streamListener
    );

    container.start();
    return subscription;
  }


  // Redis Pub/Sub 메시지를 처리하는 리스너 설정
  @Bean
  public RedisMessageListenerContainer redisMessageListener(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter listenerAdapter,
      ChannelTopic channelTopic) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter, channelTopic);
    return container;
  }

  @Bean
  public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "handleMessage");
  }

  @Bean
  public ChannelTopic channelTopic() {
    return new ChannelTopic("sse-channel");
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);

    // ObjectMapper 커스터마이징 (Java 8 날짜/시간 지원)
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Key / Value Serializer 설정
    Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(serializer);
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(serializer);

    return redisTemplate;
  }

}
