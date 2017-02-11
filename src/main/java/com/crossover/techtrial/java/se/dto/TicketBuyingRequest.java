package com.crossover.techtrial.java.se.dto;

import com.crossover.techtrial.java.se.model.airline.Route;

/**
 * Created by kasun on 2/4/17.
 */
public class TicketBuyingRequest {

    private String accountId;
    private Integer ticketAmount;
    private Route airlineRout;

    public String getApplicantId() {
        return accountId;
    }

    public void setApplicantId(String accountId) {
        this.accountId = accountId;
    }

    public Integer getTicketAmount() {
        return ticketAmount;
    }

    public void setTicketAmount(Integer ticketAmount) {
        this.ticketAmount = ticketAmount;
    }

    public Route getAirlineRout() {
        return airlineRout;
    }

    public void setAirlineRout(Route airlineRout) {
        this.airlineRout = airlineRout;
    }
}
