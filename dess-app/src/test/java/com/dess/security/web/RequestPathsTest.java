package com.dess.security.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestPathsTest
{
    @Test
    void stripsOnlyTheContextPrefix()
    {
        assertEquals("/app/people", RequestPaths.path("/dess/app/people", "/dess"));
        assertEquals("/app/security", RequestPaths.path("/app/app/security", "/app"));
        assertEquals("/other/app", RequestPaths.path("/other/app", "/dess"));
        assertEquals("/app/people", RequestPaths.path("/app/people", ""));
        assertNull(RequestPaths.path(null, "/dess"));
    }
}
