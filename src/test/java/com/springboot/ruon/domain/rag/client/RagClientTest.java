package com.springboot.ruon.domain.rag.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.springboot.ruon.domain.rag.dto.response.PregnancyCheckResponse;
import com.springboot.ruon.domain.rag.dto.response.RagAnswerResponse;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RagClientTest {

    private static final String BASE_URL = "http://localhost:8000";

    private MockRestServiceServer server;
    private RagClient ragClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ragClient = new RagClient(builder.baseUrl(BASE_URL).build());
    }

    @Test
    void 성분_질문에_답변하고_출처를_변환한다() {
        server.expect(requestTo(BASE_URL + "/answer"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.query").value("레티놀은 안전한가요?"))
                .andExpect(jsonPath("$.top_k").value(6))
                .andRespond(withSuccess("""
                        {
                          "answer": "임신 중에는 주의가 필요합니다.",
                          "sources": [{"inci_name": "Retinol", "kor_name": "레티놀"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        RagAnswerResponse response = ragClient.answer("레티놀은 안전한가요?", 6);

        assertThat(response.answer()).contains("주의");
        assertThat(response.sources()).containsExactly(new RagAnswerResponse.Source("Retinol", "레티놀"));
        server.verify();
    }

    @Test
    void 전성분표의_임신_안전성_결과를_변환한다() {
        server.expect(requestTo(BASE_URL + "/pregnancy-check"))
                .andExpect(jsonPath("$.ingredients[0]").value("레티놀"))
                .andRespond(withSuccess("""
                        {
                          "totalChecked": 2,
                          "pregnancySafe": false,
                          "warnings": [{
                            "input": "레티놀",
                            "kor_name": "레티놀",
                            "inci_name": "Retinol",
                            "reason": "임신 중 주의"
                          }],
                          "unknownIngredients": ["미확인성분"]
                        }
                        """, MediaType.APPLICATION_JSON));

        PregnancyCheckResponse response = ragClient.checkPregnancySafety(List.of("레티놀", "미확인성분"));

        assertThat(response.totalChecked()).isEqualTo(2);
        assertThat(response.pregnancySafe()).isFalse();
        assertThat(response.warnings().getFirst().inciName()).isEqualTo("Retinol");
        assertThat(response.unknownIngredients()).containsExactly("미확인성분");
        server.verify();
    }

    @Test
    void Python_서비스의_HTTP_오류를_공통_예외로_변환한다() {
        server.expect(requestTo(BASE_URL + "/answer")).andRespond(withServerError());

        assertThatThrownBy(() -> ragClient.answer("질문", 6))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RAG_SERVICE_ERROR);
        server.verify();
    }
}
