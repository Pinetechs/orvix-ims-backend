package com.pinetechs.orvix.ims.Settings.service;

import com.pinetechs.orvix.ims.Settings.entity.Settings;
import com.pinetechs.orvix.ims.Settings.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class SettingsServices {

    @Autowired
    private final SettingsRepository settingsRepository ;


    public SettingsServices(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public Settings getValueByName(String OptionName){
        return settingsRepository.findByName(OptionName);
    }


    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(settingsRepository.getAllSettings());

    }

    public void save(Settings settings) {
        settingsRepository.save(settings);
    }
}
