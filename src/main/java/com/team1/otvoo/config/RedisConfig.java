package com.team1.otvoo.config;

import com.team1.otvoo.sse.event.RedisSubscriber;
import com.team1.otvoo.sse.model.SseMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

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

    // SseMessage 객체를 JSON 형태로 직렬화
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(SseMessage.class));

    return redisTemplate;
  }

}
