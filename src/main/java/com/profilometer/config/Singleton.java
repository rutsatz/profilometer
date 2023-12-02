package com.profilometer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilometer.service.ConfigService;

public class Singleton {

    public static final ObjectMapper objectMapper = objectMapper();
    public static final ConfigService configService = configService();

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper;
    }

    private static ConfigService configService() {
        return new ConfigService();
    }

}
