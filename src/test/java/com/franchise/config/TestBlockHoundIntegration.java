package com.franchise.config;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

public class TestBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside("org.springframework.util.JdkIdGenerator", "generateId")
                .allowBlockingCallsInside("java.util.UUID", "randomUUID")
                .allowBlockingCallsInside(
                        "org.springframework.core.type.classreading.SimpleMetadataReaderFactory",
                        "getMetadataReader"
                )
                .allowBlockingCallsInside("org.springdoc.api.AbstractOpenApiResource", "getOpenApi")
                .allowBlockingCallsInside("java.io.RandomAccessFile", "readBytes")
                .allowBlockingCallsInside("java.io.FileInputStream", "readBytes");
    }
}
