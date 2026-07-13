package com.wambe.scanner;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClamAvClient {

    private final String host;
    private final int port;

    public ClamAvClient(
            @Value("${wambe.scanner.clamd-host}") String host,
            @Value("${wambe.scanner.clamd-port}") int port) {
        this.host = host;
        this.port = port;
    }

    public Verdict scan(byte[] content) {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3_000);
            socket.setSoTimeout(30_000);
            var output = new DataOutputStream(socket.getOutputStream());
            output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            int offset = 0;
            while (offset < content.length) {
                int chunk = Math.min(8192, content.length - offset);
                output.writeInt(chunk);
                output.write(content, offset, chunk);
                offset += chunk;
            }
            output.writeInt(0);
            output.flush();

            var response = new ByteArrayOutputStream();
            int value;
            while ((value = socket.getInputStream().read()) >= 0 && value != 0) {
                response.write(value);
            }
            String result = response.toString(StandardCharsets.UTF_8);
            if (result.contains("FOUND")) {
                return Verdict.INFECTED;
            }
            if (result.endsWith("OK")) {
                return Verdict.CLEAN;
            }
            return Verdict.ERROR;
        } catch (Exception exception) {
            return Verdict.ERROR;
        }
    }

    public enum Verdict {
        CLEAN,
        INFECTED,
        ERROR
    }
}
