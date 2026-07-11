package com.api2convert.resource;

import com.api2convert.http.Transport;

/**
 * API usage statistics. The response shape is free-form, so these return the decoded body as-is
 * (a {@code Map} or {@code List}).
 *
 * <p>{@code filter} is {@code single} (only the calling API key) or {@code all} (every key on the
 * account, the default). The request is scoped by the {@code X-Oc-Api-Key} header, so never pass a
 * key as {@code filter}.
 */
public final class StatsResource {

    private final Transport transport;

    public StatsResource(Transport transport) {
        this.transport = transport;
    }

    /** @param day format {@code yyyy-mm-dd} */
    public Object day(String day, String filter) {
        return transport.request("GET",
                "/stats/day/" + Transport.encodeSegment(day) + "/" + Transport.encodeSegment(filter));
    }

    public Object day(String day) {
        return day(day, "all");
    }

    /** @param month format {@code yyyy-mm} */
    public Object month(String month, String filter) {
        return transport.request("GET",
                "/stats/month/" + Transport.encodeSegment(month) + "/" + Transport.encodeSegment(filter));
    }

    public Object month(String month) {
        return month(month, "all");
    }

    /** @param year format {@code yyyy} */
    public Object year(String year, String filter) {
        return transport.request("GET",
                "/stats/year/" + Transport.encodeSegment(year) + "/" + Transport.encodeSegment(filter));
    }

    public Object year(String year) {
        return year(year, "all");
    }
}
