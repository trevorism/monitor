package com.trevorism.gcloud.webapi.controller

import org.junit.Test

class MonitorControllerTest  {

    @Test
    void testParseFrequencyFromName(){
        MonitorController mc = new MonitorController()
        assert "weekly" == mc.parseFrequencyFromName("monitor_test_weekly")
        assert "hourly" == mc.parseFrequencyFromName("monitor_testing_hourly")
        assert "daily" == mc.parseFrequencyFromName("monitor_test-xyz_daily")
    }

    @Test
    void testParseSourceFromName(){
        MonitorController mc = new MonitorController()
        assert "test" == mc.parseSourceFromName("monitor_test_weekly")
        assert "testing" == mc.parseSourceFromName("monitor_testing_hourly")
        assert "test-xyz" == mc.parseSourceFromName("monitor_test-xyz_daily")
    }

    void testCreateMonitor() {
    }

    void testListAllMonitor() {
    }

    void testGetMonitor() {
    }

    void testInvokeMonitor() {
    }

    void testRemoveMonitor() {
    }
}
