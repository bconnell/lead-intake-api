package com.leadintake.api.lead.service;

public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException() {
        super("Lead not found.");
    }
}
