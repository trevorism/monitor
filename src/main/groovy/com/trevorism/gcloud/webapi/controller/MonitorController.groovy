package com.trevorism.gcloud.webapi.controller

import com.trevorism.data.PingingDatastoreRepository
import com.trevorism.data.Repository
import com.trevorism.data.model.filtering.ComplexFilter
import com.trevorism.data.model.filtering.FilterConstants
import com.trevorism.data.model.filtering.SimpleFilter
import com.trevorism.gcloud.webapi.model.Monitor
import com.trevorism.gcloud.webapi.model.MonitorNotFoundException
import com.trevorism.gcloud.webapi.model.TestSuite
import com.trevorism.https.DefaultSecureHttpClient
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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api("Monitor Operations")
@Path("monitor")
class MonitorController {

    private SecureHttpClient httpClient = new DefaultSecureHttpClient()
    private Repository<TestSuite> testSuiteRepository = new PingingDatastoreRepository<>(TestSuite, httpClient)
    private ScheduleService scheduleService = new DefaultScheduleService()

    @ApiOperation(value = "Creates a new monitor **Secure")
    @Secure(Roles.USER)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Monitor createMonitor(Monitor monitor) {
        TestSuite testSuite = findTestSuite(monitor.source)
        ScheduledTask scheduledTask = createScheduledTask(testSuite, monitor)
        scheduleService.create(scheduledTask)
        if(monitor.frequency?.toLowerCase() != "daily" && monitor.frequency?.toLowerCase() != "weekly"){
            monitor.frequency = "weekly"
        }
        return monitor
    }

    @ApiOperation(value = "Lists all monitors **Secure")
    @Secure(Roles.USER)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Monitor> listAllMonitor() {
        scheduleService.list().findAll{
            it.name.startsWith("monitor_")
        }.collect{
            monitorFromScheduledTask(it)
        }
    }

    @ApiOperation(value = "Gets monitors based on the service name **Secure")
    @Secure(Roles.USER)
    @GET
    @Path("{source}")
    @Produces(MediaType.APPLICATION_JSON)
    Monitor getMonitor(@PathParam("source") String source) {
        monitorFromScheduledTask(scheduleService.list().find{ it.name.startsWith("monitor_${source}_")})
    }

    @ApiOperation(value = "Invoke the monitor **Secure")
    @Secure(value = Roles.USER, allowInternal = true)
    @POST
    @Path("{source}")
    @Produces(MediaType.APPLICATION_JSON)
    Monitor invokeMonitor(@PathParam("source") String source) {
        TestSuite testSuite = findTestSuite(source)
        httpClient.post("https://testing.trevorism.com/api/${testSuite.id}", "{}")
        getMonitor(source)
    }

    @ApiOperation(value = "Remove registered monitor **Secure")
    @Secure(Roles.USER)
    @DELETE
    @Path("{source}")
    @Produces(MediaType.APPLICATION_JSON)
    Monitor removeMonitor(@PathParam("source") String source) {
        Monitor monitor = getMonitor(source)
        scheduleService.delete("monitor_${source}_${monitor.frequency}")
        return monitor
    }

    private static String parseSourceFromName(String s) {
        s["monitor_".length()..s.lastIndexOf("_")-1]
    }

    private static String parseFrequencyFromName(String s) {
        s[s.lastIndexOf("_")+1..-1]
    }

    private Monitor monitorFromScheduledTask(ScheduledTask it) {
        if(!it){
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
        List<TestSuite> list =  testSuiteRepository.filter(filter)
        if(!list)
            throw new MonitorNotFoundException("Unable to locate cucumber test suite with source: ${monitor.source}")
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
