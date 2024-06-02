package com.trevorism.gcloud.webapi.controller

import com.trevorism.http.HeadersHttpResponse
import com.trevorism.http.HttpClient
import com.trevorism.https.SecureHttpClient
import com.trevorism.https.token.ObtainTokenStrategy

class StubSecureHttpClient implements SecureHttpClient{
    @Override
    ObtainTokenStrategy getObtainTokenStrategy() {
        return null
    }

    @Override
    HttpClient getHttpClient() {
        return null
    }

    @Override
    String get(String s) {
        return null
    }

    @Override
    HeadersHttpResponse get(String s, Map<String, String> map) {
        return null
    }

    @Override
    String post(String s, String s1) {
        return null
    }

    @Override
    HeadersHttpResponse post(String s, String s1, Map<String, String> map) {
        return null
    }

    @Override
    String put(String s, String s1) {
        return null
    }

    @Override
    HeadersHttpResponse put(String s, String s1, Map<String, String> map) {
        return null
    }

    @Override
    String patch(String s, String s1) {
        return null
    }

    @Override
    HeadersHttpResponse patch(String s, String s1, Map<String, String> map) {
        return null
    }

    @Override
    String delete(String s) {
        return null
    }

    @Override
    HeadersHttpResponse delete(String s, Map<String, String> map) {
        return null
    }
}
