package com.springboot.ruon.domain.rag.service;

import com.springboot.ruon.domain.rag.client.RagClient;
import com.springboot.ruon.domain.rag.dto.request.PregnancyCheckRequest;
import com.springboot.ruon.domain.rag.dto.request.RagAnswerRequest;
import com.springboot.ruon.domain.rag.dto.response.PregnancyCheckResponse;
import com.springboot.ruon.domain.rag.dto.response.RagAnswerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagService {

    private final RagClient ragClient;

    public RagAnswerResponse answer(RagAnswerRequest request) {
        return ragClient.answer(request.query(), request.topK());
    }

    public PregnancyCheckResponse checkPregnancySafety(PregnancyCheckRequest request) {
        return ragClient.checkPregnancySafety(request.ingredients());
    }
}
