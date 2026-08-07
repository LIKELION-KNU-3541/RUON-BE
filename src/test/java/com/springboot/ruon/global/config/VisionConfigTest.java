package com.springboot.ruon.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

/**
 * Vision 설정이 실제 스프링 컨텍스트에서 생성되는지 검증한다.
 * <p>
 * 서비스 단위 테스트는 RestClient를 직접 만들어 쓰므로 빈 생성 실패를 잡지 못한다.
 * 실제로 spring-boot-restclient 모듈이 없어 RestClient.Builder 주입이 실패하는 문제가
 * 기동 시점에야 드러난 적이 있어, 그 회귀를 막기 위한 테스트다.
 */
class VisionConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(VisionConfig.class);

    @Test
    @DisplayName("API 키가 설정되면 visionRestClient 빈이 생성된다")
    void 빈_생성() {
        contextRunner
                .withPropertyValues("google.vision.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RestClient.class);
                });
    }

    @Test
    @DisplayName("API 키가 없어도 컨텍스트는 정상 기동한다")
    void 키_없이도_기동() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RestClient.class);
            assertThat(context.getBean(VisionProperties.class).hasApiKey()).isFalse();
        });
    }

    @Test
    @DisplayName("엔드포인트와 타임아웃은 설정이 없으면 기본값을 사용한다")
    void 기본값() {
        contextRunner.run(context -> {
            VisionProperties properties = context.getBean(VisionProperties.class);
            assertThat(properties.endpoint()).isEqualTo("https://vision.googleapis.com/v1/images:annotate");
            assertThat(properties.connectTimeout()).isNotNull();
            assertThat(properties.readTimeout()).isNotNull();
        });
    }
}
