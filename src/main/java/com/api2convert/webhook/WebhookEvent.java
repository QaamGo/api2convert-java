package com.api2convert.webhook;

import com.api2convert.model.Job;
import java.util.Map;

/**
 * A verified webhook callback. The API posts the job whose status changed.
 *
 * @param job     the job whose status changed
 * @param payload the full decoded callback body
 */
public record WebhookEvent(Job job, Map<String, Object> payload) {

    public static WebhookEvent fromMap(Map<String, Object> payload) {
        return new WebhookEvent(Job.fromMap(payload), payload);
    }
}
