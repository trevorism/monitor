package com.trevorism.gcloud.webapi.model

class TestSuite {

    String id
    //Easily identify the test suite
    String name
    //Type of test suite
    String kind
    //Where the test suite lives
    String source

    boolean lastRunSuccess
    Date lastRunDate
    long lastRuntimeSeconds
}
