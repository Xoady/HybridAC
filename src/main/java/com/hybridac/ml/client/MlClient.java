package com.hybridac.ml.client;

import com.hybridac.ml.dto.DatasetUploadRequest;
import com.hybridac.ml.dto.DatasetUploadResponse;
import com.hybridac.ml.dto.InferenceRequest;
import com.hybridac.ml.dto.InferenceResponse;

import java.util.concurrent.CompletableFuture;

public final class MlClient {

    private final MlRequestExecutor requestExecutor;

    public MlClient(MlRequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }

    public CompletableFuture<MlHttpResult<DatasetUploadResponse>> uploadDataset(DatasetUploadRequest request) {
        return requestExecutor.postJson("/dataset/upload", request, DatasetUploadResponse.class);
    }

    public CompletableFuture<MlHttpResult<InferenceResponse>> infer(InferenceRequest request) {
        return requestExecutor.postJson("/infer", request, InferenceResponse.class);
    }
}
