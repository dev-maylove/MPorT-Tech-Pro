package com.mporttech.pro.features.networktools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkOutputParserTest {
    @Test fun `parsePortList removes invalid and duplicate ports`() {
        assertEquals(listOf(53, 80, 443), NetworkOutputParser.parsePortList("53,80,443,80,0,70000"))
    }

    @Test fun `parsePingOutput handles timeout without crash`() {
        val result = NetworkOutputParser.parsePingOutput("Request timed out\n100% packet loss", 1)
        assertEquals(100.0, result.lossPct, 0.0)
        assertTrue(result.samples.any { !it.success })
    }
}
