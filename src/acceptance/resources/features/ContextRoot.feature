Feature: Context Root of this API
  In order to use the API, it must be available

  Scenario: HTTP GET on the ContextRoot
    Given the application is alive
    When I navigate to "https://monitor.testing.trevorism.com"
    Then then a link to the help page is displayed

  Scenario: Ping the application
    Given the application is alive
    When I ping the application deployed to "https://monitor.testing.trevorism.com"
    Then pong is returned, to indicate the service is alive