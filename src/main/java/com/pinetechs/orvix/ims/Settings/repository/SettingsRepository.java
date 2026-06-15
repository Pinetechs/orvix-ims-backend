package com.pinetechs.orvix.ims.Settings.repository;

import com.pinetechs.orvix.ims.Settings.entity.Settings;
import org.apache.catalina.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

@EnableJpaRepositories
public interface SettingsRepository extends JpaRepository<Settings, Long> {
    Settings findByName(String optionName);

    @Query("select s from Settings s where upper(s.name) not like upper('%password%')")
    List<Settings> getAllSettings();


}