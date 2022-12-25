package com.trevorism.gcloud.webapi.model

import javax.ws.rs.WebApplicationException

class MonitorNotFoundException extends WebApplicationException{
    MonitorNotFoundException(String message){
        super(message, 400)
    }
}
