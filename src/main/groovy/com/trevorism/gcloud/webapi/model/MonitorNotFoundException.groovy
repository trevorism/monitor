package com.trevorism.gcloud.webapi.model

class MonitorNotFoundException extends RuntimeException{
    MonitorNotFoundException(String message){
        super(message)
    }
}
