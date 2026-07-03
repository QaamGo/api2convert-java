package com.api2convert.resource;

import com.api2convert.http.Transport;

/**
 * Information about the account's active contracts. Free-form response, returned as the decoded
 * body ({@code Map} or {@code List}).
 */
public final class ContractsResource {

    private final Transport transport;

    public ContractsResource(Transport transport) {
        this.transport = transport;
    }

    public Object get() {
        return transport.request("GET", "/contracts");
    }
}
