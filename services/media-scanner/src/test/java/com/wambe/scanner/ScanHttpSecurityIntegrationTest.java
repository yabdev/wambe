package com.wambe.scanner;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScanHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScanService scanService;

    @Test
    void oversizedScanIsRejectedBeforeHmacAndController() throws Exception {
        mockMvc.perform(post("/scan")
                        .header("Content-Length", 16_385)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isPayloadTooLarge());

        verifyNoInteractions(scanService);
    }

    @Test
    void chunkedOversizedScanIsRejectedBeforeController() throws Exception {
        mockMvc.perform(post("/scan")
                        .header("Transfer-Encoding", "chunked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[16_385]))
                .andExpect(status().isPayloadTooLarge());

        verifyNoInteractions(scanService);
    }

    @Test
    void validSizeWithMissingHmacRemainsUnauthorized() throws Exception {
        mockMvc.perform(post("/scan")
                        .header("Content-Length", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(scanService);
    }
}
