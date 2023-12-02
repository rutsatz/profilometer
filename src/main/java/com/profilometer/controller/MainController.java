package com.profilometer.controller;

import com.profilometer.config.Singleton;
import com.profilometer.service.ConfigService;

public class MainController {

    private final ConfigService configService = Singleton.configService;


    public void menuFileOpen() {
        ConfigService.chooseInputFolder();
    }

}
