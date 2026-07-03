package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.model.Job;
import org.junit.jupiter.api.Test;

class JobModelTest extends A2CTestCase {

    @Test
    void hydratesFromApiPayload() {
        Job job = Job.fromMap(parse("""
                {"id":"8daae6d1-26e0-11e5-b2a1-0800273b325b","token":"tok",
                 "server":"https://www2.api2convert.com/v2",
                 "status":{"code":"completed","info":"done"},
                 "conversion":[{"target":"png","category":"image","options":{"quality":85}}],
                 "input":[{"id":"in","type":"remote","source":"https://x/y.jpg"}],
                 "output":[{"id":"o","uri":"https://dl/result.png","filename":"result.png","size":2048}],
                 "warnings":[{"code":1,"message":"heads up"}]}
                """));

        assertEquals("8daae6d1-26e0-11e5-b2a1-0800273b325b", job.id());
        assertEquals("tok", job.token());
        assertTrue(job.isCompleted());
        assertFalse(job.isFailed());
        assertTrue(job.isTerminal());
        assertEquals("png", job.conversion().get(0).target());
        assertEquals(85, job.conversion().get(0).options().get("quality"));
        assertEquals("remote", job.input().get(0).type());
        assertEquals(2048L, job.output().get(0).size());
        assertEquals("result.png", job.output().get(0).filename());
        assertEquals(1, job.warnings().size());
        assertEquals(0, job.errors().size());
    }

    @Test
    void unknownStatusIsNonTerminal() {
        Job job = Job.fromMap(parse("{\"id\":\"j\",\"status\":{\"code\":\"something_new\"}}"));

        assertFalse(job.isTerminal());
        assertFalse(job.isCompleted());
    }

    @Test
    void toleratesMissingFields() {
        Job job = Job.fromMap(parse("{\"id\":\"j\",\"status\":{\"code\":\"incomplete\"}}"));

        assertTrue(job.output().isEmpty());
        assertNull(job.token());
    }
}
