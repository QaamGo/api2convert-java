package com.api2convert.http;

import java.io.InputStream;

/**
 * A transport-agnostic HTTP response. The body is exposed as a stream: callers that need the whole
 * body (JSON) read it fully; the download path consumes it in chunks. The body is read at most once.
 */
public interface Response {

    int status();

    /** First value of the given header, or {@code ""} if absent. Case-insensitive. */
    String header(String name);

    /** The response body stream. The caller owns it and must close it. */
    InputStream body();
}
