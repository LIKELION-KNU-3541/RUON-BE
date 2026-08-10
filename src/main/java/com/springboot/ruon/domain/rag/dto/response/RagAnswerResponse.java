package com.springboot.ruon.domain.rag.dto.response;

import java.util.List;

public record RagAnswerResponse(String answer, List<Source> sources) {

    public record Source(String inciName, String korName) {
    }
}
