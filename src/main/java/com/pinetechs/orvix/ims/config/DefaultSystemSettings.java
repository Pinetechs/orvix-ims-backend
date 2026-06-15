package com.pinetechs.orvix.ims.config;

import com.pinetechs.orvix.ims.Settings.entity.Settings;
import com.pinetechs.orvix.ims.Settings.service.SettingsServices;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class DefaultSystemSettings  implements CommandLineRunner {

    private final SettingsServices settingsServices;

    public DefaultSystemSettings(SettingsServices settingsServices) {
        this.settingsServices = settingsServices;
    }

    @Override
    public void run(String... args) throws Exception {

        boolean exists = settingsServices.getValueByName("companyLogo") != null;
        if (exists) return;


        Settings settings = new Settings();
        settings.setName("companyLogo");
        settings.setValue("public/upload/Settings/logo.png");
        settingsServices.save(settings);


    }
}
