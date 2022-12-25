package com.trevorism.gcloud.webapi.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.Contact
import io.swagger.annotations.Info
import io.swagger.annotations.SwaggerDefinition

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api("Root Operations")
@SwaggerDefinition(
        info = @Info(
                description = "API",
                version = "1",
                title = "API",
                contact = @Contact(name = "Trevor Brooks", url = "https://www.trevorism.com")
        )
)
@Path("/")
class RootController {

    @ApiOperation(value = "Returns 'pong' if the application is alive")
    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    String ping() {
        "pong"
    }

    @ApiOperation(value = "Context root of the application")
    @GET
    String displayHelpLink(){
        '<h1>API</h1><br/>Visit the help page at <a href="/help">/help'
    }

    @ApiOperation(value = "Shows this help page")
    @GET
    @Path("help")
    Response help(){
        Response.temporaryRedirect(new URI("/swagger/index.html")).build()
    }
}
