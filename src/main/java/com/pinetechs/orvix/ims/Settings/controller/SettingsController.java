package com.pinetechs.orvix.ims.Settings.controller;

import com.pinetechs.orvix.ims.Settings.service.SettingsServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsServices settingsServices;

    @GetMapping("/all")
    public ResponseEntity<?> getAllSettings() {
        return settingsServices.getAll();
    }
}
