package com.pinetechs.orvix.ims.config;

import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.UserType;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultSystemAdminSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultSystemAdminSeeder(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean exists = userRepository.existsByUserTypeAndDeletedFalse(UserType.SYSTEM_ADMIN);
        if (exists) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setEmail("admin@orvix.local");
        admin.setUserType(UserType.SYSTEM_ADMIN);
        admin.setAccessChannel(AccessChannel.WEB);
        admin.setEnabled(true);
        admin.setDeleted(false);
        admin.setCompanies(null);

        userRepository.save(admin);


    }
}
