package com.api2convert.http;

import java.io.IOException;

/**
 * The pluggable HTTP transport seam. The default is {@link JdkHttpSender} (backed by the JDK's
 * {@code java.net.http.HttpClient}); tests inject a fake that serves canned responses.
 *
 * <p>An implementation must honor {@link Request#followRedirects()} <em>per request</em>: never
 * follow a redirect on an authenticated request (that would leak the account key / per-job token,
 * which travel in custom headers, to the redirect target), and follow redirects only on the
 * self-contained download path. Retry/backoff and error mapping live in {@code Transport}, not here.
 */
@FunctionalInterface
public interface HttpSender {

    Response send(Request request) throws IOException;
}
