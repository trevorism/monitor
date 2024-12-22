package com.trevorism.gcloud.webapi.controller

import com.trevorism.data.FastDatastoreRepository
import com.trevorism.data.Repository
import com.trevorism.data.model.filtering.FilterBuilder
import com.trevorism.data.model.filtering.FilterConstants
import com.trevorism.data.model.filtering.SimpleFilter
import com.trevorism.gcloud.webapi.model.Monitor
import com.trevorism.gcloud.webapi.model.MonitorNotFoundException
import com.trevorism.gcloud.webapi.model.TestSuite
import com.trevorism.https.SecureHttpClient
import com.trevorism.schedule.DefaultScheduleService
import com.trevorism.schedule.ScheduleService
import com.trevorism.schedule.factory.DefaultScheduledTaskFactory
import com.trevorism.schedule.factory.EndpointSpec
import com.trevorism.schedule.factory.ScheduledTaskFactory
import com.trevorism.schedule.model.HttpMethod
import com.trevorism.schedule.model.ScheduledTask
import com.trevorism.secure.Roles
import com.trevorism.secure.Secure
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/monitor")
class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController)
    private SecureHttpClient httpClient
    private Repository<TestSuite> testSuiteRepository
    private Repository<Monitor> monitorRepository
    private ScheduleService scheduleService

    MonitorController(SecureHttpClient httpClient) {
        this.httpClient = httpClient
        testSuiteRepository = new FastDatastoreRepository<>(TestSuite.class, httpClient)
        monitorRepository = new FastDatastoreRepository<>(Monitor.class, httpClient)
        scheduleService = new DefaultScheduleService(httpClient)
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Creates a new monitor **Secure")
    @Secure(value = Roles.USER)
    @Post(value = "/", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    Monitor createMonitor(@Body Monitor monitor) {
        TestSuite testSuite = findTestSuite(monitor)
        ScheduledTask scheduledTask = scheduleService.create(createScheduledTask(testSuite, monitor))
        monitor.scheduleId = scheduledTask.id
        monitor.testSuiteId = testSuite.id
        return monitorRepository.create(monitor)
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Lists all monitors **Secure")
    @Secure(Roles.USER)
    @Get(value = "/", consumes = MediaType.APPLICATION_JSON)
    List<Monitor> listAllMonitors() {
        monitorRepository.list()
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Get monitor by id **Secure")
    @Secure(value = Roles.USER, allowInternal = true)
    @Get(value = "/{id}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    Monitor getMonitor(String id) {
        Monitor monitor = monitorRepository.get(id)
        return monitor
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Invoke the monitor **Secure")
    @Secure(value = Roles.USER, allowInternal = true)
    @Post(value = "/{id}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    Monitor invokeMonitor(String id) {
        Monitor monitor = monitorRepository.get(id)
        TestSuite testSuite = findTestSuite(monitor)
        httpClient.post("https://testing.trevorism.com/api/suite/${testSuite.id}", "{}")
        return monitor
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Remove registered monitor **Secure")
    @Delete(value = "{id}", produces = MediaType.APPLICATION_JSON)
    @Secure(value = Roles.USER)
    Monitor removeMonitor(String id) {
        Monitor monitor = monitorRepository.get(id)
        try {
            scheduleService.delete(monitor.scheduleId)
        } catch (Exception e) {
            log.warn("Unable to delete schedule for monitor: ${monitor.id}", e)
        }
        return monitorRepository.delete(id)
    }

    private TestSuite findTestSuite(Monitor monitor) {
        if (monitor.testSuiteId)
            return testSuiteRepository.get(monitor.testSuiteId)

        def filter = new FilterBuilder()
                .addFilter(new SimpleFilter("source", FilterConstants.OPERATOR_EQUAL, monitor.source))
                .addFilter(new SimpleFilter("kind", FilterConstants.OPERATOR_EQUAL, monitor.kind))
                .build()
        List<TestSuite> list = testSuiteRepository.filter(filter)
        if (!list)
            throw new MonitorNotFoundException("Unable to locate cucumber test suite with source: ${monitor.source}")
        return list.first()
    }

    private static ScheduledTask createScheduledTask(TestSuite testSuite, Monitor monitor) {
        ScheduledTaskFactory factory = new DefaultScheduledTaskFactory()
        EndpointSpec endpointSpec = new EndpointSpec("https://testing.trevorism.com/api/suite/${testSuite.id}", HttpMethod.POST, "{}")
        if (monitor.frequency == "daily") {
            return factory.createDailyTask("monitor_${monitor.source}_daily_${monitor.kind}", monitor.startDate, endpointSpec)
        } else if (monitor.frequency == "hourly") {
            return factory.createHourlyTask("monitor_${monitor.source}_hourly_${monitor.kind}", monitor.startDate, endpointSpec)
        }
        return factory.createWeeklyTask("monitor_${monitor.source}_weekly_${monitor.kind}", monitor.startDate, endpointSpec)

    }
}
