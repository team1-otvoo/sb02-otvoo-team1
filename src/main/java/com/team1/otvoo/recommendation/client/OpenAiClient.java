package com.team1.otvoo.recommendation.client;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.recommendation.dto.VisionAttributeResponseDto;
import java.net.MalformedURLException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@RequiredArgsConstructor
@Component
@Slf4j
public class OpenAiClient {

  private final ChatClient chatClient;

  public VisionAttributeResponseDto analyzeImage(String imageUrl) {
    BeanOutputConverter<VisionAttributeResponseDto> parser = new BeanOutputConverter<>(
        VisionAttributeResponseDto.class);

    String response = chatClient.prompt()
        .user(userSpec -> {
          try {
            userSpec.text(
                    """
                    <role>
                    의류 사진을 주면, 제공된 format에 맞춰 응답해야 한다.
                    </role>
                    <instruction>
                    1. 예시 속성과 다른 속성도 폭넓게 가져와야만 한다.
                    2. 속성은 반드시 날씨와 관련된 속성으로만 가져와야 한다.
                    2-1. 날씨와 관련된 속성이란, 덥고 추울 때 / 비올 때나 맑을 때 선호도가 달라지는 의상 속성을 이야기하는 것.
                    2-2. uvProtection, dryTime과 같은 너무 특이한 속성이 아닌, 보편적인 속성을 가져올 것.
                    3. 계절(season), 두께(thickness), 소매 길이(sleevsLength), 방수(waterProof) 속성은 제외해야한다.
                    4. 속성 개수는 5개로 제한한다.
                    5. 색상,핏감과 같은 날씨와 상관없는 속성은 제외하고 가져와야 한다.
                    </instruction>
                    <example>
                    예시 응답을 알려주겠다. 예시 응답과 같은 의류가 나오더라도 이 예시 값을 똑같이 활용하지는 마라.
                    {
                        "AiResponse": [
                            {
                                "color": "파란색",
                                "sleevesLength": "짧음",
                                "neck": "카라있음",
                                "width": "500",
                                "length": "300"
                            },
                        ]
                    }
                    </example>
                    format은 아래와 같다.
                    """ + parser.getFormat())
                .media(MimeTypeUtils.IMAGE_JPEG, new UrlResource(imageUrl));
          } catch (MalformedURLException e) {
            log.warn("URL 형식이 올바르지 않습니다 - url: {}", imageUrl);
            throw new RestException(ErrorCode.MALFORMED_URL, Map.of("url", imageUrl));
          }
        })
        .call()
        .content();

    return parser.convert(response);
  }
}
