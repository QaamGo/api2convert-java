package com.api2convert.resource;

import com.api2convert.http.Transport;

/**
 * API usage statistics. The response shape is free-form, so these return the decoded body as-is
 * (a {@code Map} or {@code List}).
 *
 * <p>{@code filter} is either an API key to scope to, or {@code all}.
 */
public final class StatsResource {

    private final Transport transport;

    public StatsResource(Transport transport) {
        this.transport = transport;
    }

    /** @param day format {@code yyyy-mm-dd} */
    public Object day(String day, String filter) {
        return transport.request("GET", "/stats/day/" + day + "/" + filter);
    }

    public Object day(String day) {
        return day(day, "all");
    }

    /** @param month format {@code yyyy-mm} */
    public Object month(String month, String filter) {
        return transport.request("GET", "/stats/month/" + month + "/" + filter);
    }

    public Object month(String month) {
        return month(month, "all");
    }

    /** @param year format {@code yyyy} */
    public Object year(String year, String filter) {
        return transport.request("GET", "/stats/year/" + year + "/" + filter);
    }

    public Object year(String year) {
        return year(year, "all");
    }
}
