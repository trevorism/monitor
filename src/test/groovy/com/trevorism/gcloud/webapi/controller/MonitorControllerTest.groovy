package com.trevorism.gcloud.webapi.controller

import com.trevorism.data.Repository
import com.trevorism.gcloud.webapi.model.Monitor
import com.trevorism.gcloud.webapi.model.MonitorNotFoundException
import com.trevorism.gcloud.webapi.model.TestSuite
import com.trevorism.https.SecureHttpClient
import com.trevorism.schedule.ScheduleService
import com.trevorism.schedule.model.ScheduledTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.assertThrows;


class MonitorControllerTest {

    public static final String SOURCE_FOR_TEST = "testing"
    private MonitorController mc

    @BeforeEach
    void setUp() {
        mc = new MonitorController(new StubSecureHttpClient())
    }

    @Test
    void testCreateMonitor() {
        mc.testSuiteRepository = ["filter": { x -> [new TestSuite()] }] as Repository
        mc.scheduleService = [create: { x -> new ScheduledTask() }] as ScheduleService
        mc.monitorRepository = ["create": { x -> x }] as Repository

        Monitor monitor = mc.createMonitor(new Monitor(source: SOURCE_FOR_TEST))
        assert monitor.source == SOURCE_FOR_TEST
        assert monitor.frequency == "weekly"
        assert monitor.startDate
    }

    @Test
    void testCreateMonitorInvalid() {
        mc.testSuiteRepository = ["filter": { x -> [] }] as Repository
        mc.scheduleService = [create: { x -> new ScheduledTask() }] as ScheduleService

        MonitorNotFoundException exception = assertThrows(MonitorNotFoundException.class, () -> {
            mc.createMonitor(new Monitor(source: SOURCE_FOR_TEST))
        })
        assert exception.getMessage() == "Unable to locate cucumber test suite with source: ${SOURCE_FOR_TEST}"
    }


    @Test
    void testListAllMonitor() {
        mc.scheduleService = [list: { ->
            [new ScheduledTask(name: "monitor_${SOURCE_FOR_TEST}_hurry", startDate: new Date())] }] as ScheduleService
        mc.monitorRepository = ["list": { -> [new Monitor(source: SOURCE_FOR_TEST, frequency: "hurry")] }] as Repository
        def list = mc.listAllMonitors()
        assert list
        Monitor monitor = list[0]
        assert monitor.source == SOURCE_FOR_TEST
        assert monitor.frequency == "hurry"
        assert monitor.startDate
    }

    @Test
    void testGetMonitor() {
        mc.monitorRepository = ["get": { x -> new Monitor(source: SOURCE_FOR_TEST, frequency: "hourly", startDate: new Date()) }] as Repository
        Monitor monitor = mc.getMonitor(SOURCE_FOR_TEST)
        assert monitor.source == SOURCE_FOR_TEST
        assert monitor.frequency == "hourly"
        assert monitor.startDate
    }

    @Test
    void testInvokeMonitor() {
        mc.testSuiteRepository = ["filter": { x -> [new TestSuite()] }] as Repository
        mc.httpClient = [post: { x, y -> null }] as SecureHttpClient
        mc.monitorRepository = ["get": { x -> new Monitor(source: SOURCE_FOR_TEST, frequency: "daily", startDate: new Date()) }] as Repository
        Monitor monitor = mc.invokeMonitor(SOURCE_FOR_TEST)
        assert monitor.source == SOURCE_FOR_TEST
        assert monitor.frequency == "daily"
        assert monitor.startDate
    }

    @Test
    void testRemoveMonitor() {
        mc.scheduleService = [delete: { x -> new ScheduledTask() }] as ScheduleService
        mc.monitorRepository = ["get": { x -> new Monitor(source: SOURCE_FOR_TEST, frequency: "daily", startDate: new Date()) },
        "delete": { x -> new Monitor(source: SOURCE_FOR_TEST, frequency: "daily", startDate: new Date())}] as Repository
        Monitor monitor = mc.removeMonitor(SOURCE_FOR_TEST)
        assert monitor.source == SOURCE_FOR_TEST
        assert monitor.frequency == "daily"
        assert monitor.startDate
    }
}
