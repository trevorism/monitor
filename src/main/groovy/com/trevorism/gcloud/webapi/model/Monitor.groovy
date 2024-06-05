package com.trevorism.gcloud.webapi.model

class Monitor {

    String id
    String source
    String kind = "cucumber"
    String frequency = "weekly" // hourly, daily, weekly,
    Date startDate = new Date()

    String testSuiteId
    String scheduleId
}
