package com.springboot.ruon.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(springdoc-openapi) 설정.
 * 실행 후 /swagger-ui.html 에서 API 문서 + 테스트 콘솔 확인 가능.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI ruonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RUON API")
                        .description("임산부/수유부 스킨케어 루틴 추천 서비스 API 명세서")
                        .version("v1"));
    }
}
