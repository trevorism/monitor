package com.trevorism.gcloud.webapi.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Controller("/")
class RootController {

    private static final Logger log = LoggerFactory.getLogger(RootController)

    @Tag(name = "Root Operations")
    @Operation(summary = "Returns 'pong' if the application is alive")
    @Get(value = "/ping", produces = MediaType.TEXT_PLAIN)
    String ping() {
        "pong"
    }

    @Tag(name = "Root Operations")
    @Get(produces = MediaType.TEXT_HTML)
    @Operation(summary = "Context Root")
    HttpResponse<List<String>> index() {
        log.info("Hit context root")
        HttpResponse.ok(['<a href="/ping">/ping</a>', '<a href="/help">/help</a>'])
    }

    @Tag(name = "Root Operations")
    @Operation(summary = "This help page")
    @ApiResponse(responseCode = "302")
    @Get(value = "/help")
    HttpResponse<String> help() {
        return HttpResponse.redirect(new URI("../swagger-ui/index.html"))
    }
}