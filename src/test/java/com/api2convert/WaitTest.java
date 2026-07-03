package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.ConversionFailedException;
import com.api2convert.exception.TimeoutException;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import org.junit.jupiter.api.Test;

class WaitTest extends A2CTestCase {

    @Test
    void pollsUntilCompleted() {
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"incomplete\"}}");
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"processing\"}}");
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        Job job = client().jobs().await("j");

        assertTrue(job.isCompleted());
        assertEquals(3, http.requests.size());
    }

    @Test
    void throwsConversionFailedWithJobErrors() {
        http.addJson(200, """
                {"id":"j","status":{"code":"failed","info":"The conversion failed."},
                 "errors":[{"code":4000,"message":"The input file could not be processed."}]}
                """);

        ConversionFailedException e = assertThrows(ConversionFailedException.class,
                () -> client().jobs().await("j"));
        assertEquals(1, e.errors().size());
        assertEquals(4000, e.errors().get(0).code());
        assertTrue(e.errors().get(0).message().contains("could not be processed"));
        assertTrue(e.getJob().isFailed());
    }

    @Test
    void returnsFailedJobWhenThrowOnFailureDisabled() {
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"failed\"}}");

        Job job = client().jobs().await("j", null, false);

        assertTrue(job.isFailed());
    }

    @Test
    void timesOut() {
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"incomplete\"}}");

        assertThrows(TimeoutException.class,
                () -> client(Config.builder().pollTimeout(0).build()).jobs().await("j"));
    }
}
