package com.trevorism.gcloud.webapi.controller

import com.trevorism.data.FastDatastoreRepository
import com.trevorism.data.PingingDatastoreRepository
import com.trevorism.data.Repository
import com.trevorism.data.model.filtering.ComplexFilter
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

@Controller("/monitor")
class MonitorController {

    private SecureHttpClient httpClient
    private Repository<TestSuite> testSuiteRepository
    private ScheduleService scheduleService

    MonitorController(SecureHttpClient httpClient) {
        this.httpClient = httpClient
        testSuiteRepository = new FastDatastoreRepository<>(TestSuite.class, httpClient)
        scheduleService = new DefaultScheduleService(httpClient)
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Creates a new monitor **Secure")
    @Secure(value = Roles.USER)
    @Post(value = "/", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    Monitor createMonitor(@Body Monitor monitor) {
        TestSuite testSuite = findTestSuite(monitor.source)
        ScheduledTask scheduledTask = createScheduledTask(testSuite, monitor)
        scheduleService.create(scheduledTask)
        if (monitor.frequency?.toLowerCase() != "daily" && monitor.frequency?.toLowerCase() != "weekly") {
            monitor.frequency = "weekly"
        }
        return monitor
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Lists all monitors **Secure")
    @Secure(Roles.USER)
    @Get(value = "/", consumes = MediaType.APPLICATION_JSON)
    List<Monitor> listAllMonitor() {
        scheduleService.list().findAll {
            it.name.startsWith("monitor_")
        }.collect {
            monitorFromScheduledTask(it)
        }
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Gets monitors based on the service name **Secure")
    @Get(value = "{source}", produces = MediaType.APPLICATION_JSON)
    @Secure(value = Roles.USER)
    Monitor getMonitor(String source) {
        monitorFromScheduledTask(scheduleService.list().find { it.name.startsWith("monitor_${source}_") })
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Invoke the monitor **Secure")
    @Secure(value = Roles.USER, allowInternal = true)
    @Post(value = "/{source}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    Monitor invokeMonitor(String source, @Body Map<String, Object> map) {
        TestSuite testSuite = findTestSuite(source)
        httpClient.post("https://testing.trevorism.com/api/suite/${testSuite.id}", "{}")
        getMonitor(source)
    }

    @Tag(name = "Monitor Operations")
    @Operation(summary = "Remove registered monitor **Secure")
    @Delete(value = "{source}", produces = MediaType.APPLICATION_JSON)
    @Secure(value = Roles.USER)
    Monitor removeMonitor(String source) {
        Monitor monitor = getMonitor(source)
        scheduleService.delete("monitor_${source}_${monitor.frequency}")
        return monitor
    }

    private static String parseSourceFromName(String s) {
        s["monitor_".length()..s.lastIndexOf("_") - 1]
    }

    private static String parseFrequencyFromName(String s) {
        s[s.lastIndexOf("_") + 1..-1]
    }

    private static Monitor monitorFromScheduledTask(ScheduledTask it) {
        if (!it) {
            return new Monitor(startDate: null)
        }

        String source = parseSourceFromName(it.name)
        String frequency = parseFrequencyFromName(it.name)
        new Monitor(source: source, startDate: it.startDate, frequency: frequency)
    }

    private TestSuite findTestSuite(String source) {
        def filter = new ComplexFilter()
        filter.addSimpleFilter(new SimpleFilter("source", FilterConstants.OPERATOR_EQUAL, source))
        filter.addSimpleFilter(new SimpleFilter("kind", FilterConstants.OPERATOR_EQUAL, "cucumber"))
        List<TestSuite> list = testSuiteRepository.filter(filter)
        if (!list)
            throw new MonitorNotFoundException("Unable to locate cucumber test suite with source: ${source}")
        return list[0]
    }

    private ScheduledTask createScheduledTask(TestSuite testSuite, Monitor monitor) {
        ScheduledTaskFactory factory = new DefaultScheduledTaskFactory()
        EndpointSpec endpointSpec = new EndpointSpec("https://testing.trevorism.com/api/suite/${testSuite.id}", HttpMethod.POST, "{}")
        if (monitor.frequency == "daily") {
            return factory.createDailyTask("monitor_${monitor.source}_daily", monitor.startDate, endpointSpec)
        } else if (monitor.frequency == "hourly") {
            return factory.createHourlyTask("monitor_${monitor.source}_hourly", monitor.startDate, endpointSpec)
        }
        return factory.createWeeklyTask("monitor_${monitor.source}_weekly", monitor.startDate, endpointSpec)

    }
}
