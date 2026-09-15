package com.hybridac.ml.client;

public record MlHttpResult<T>(int statusCode, T body) {
}
