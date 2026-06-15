package com.pinetechs.orvix.ims.config;

import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import com.pinetechs.orvix.ims.user.enums.UserType;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import com.pinetechs.orvix.ims.user.service.PermissionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DefaultSystemAdminSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Logger log =
            LoggerFactory.getLogger(DefaultSystemAdminSeeder.class);


    public DefaultSystemAdminSeeder(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("Default System Admin Credentials");
        log.info("Username: admin");
        log.info("Password: admin123");

        boolean existsSystemAdmin = userRepository.existsByUserTypeAndDeletedFalse(UserType.SYSTEM_ADMIN);
        if (!existsSystemAdmin) {
            createDefaultAdmin();
        }

        boolean existsSupportUser = userRepository.existsByUserTypeAndDeletedFalse(UserType.PINETECHS_SUPPORT_STAFF);
        if(!existsSupportUser){
            createDefaultSupportUser();
        }








    }

    private void createDefaultSupportUser() {


        User admin = new User();
        admin.setUsername("abutouq");
        admin.setPassword(passwordEncoder.encode("523901402"));
        admin.setFirstName("mohammad");
        admin.setLastName("abutouq");
        admin.setEmail("tech@pinetechs.com");
        admin.setUserType(UserType.PINETECHS_SUPPORT_STAFF);
        admin.setAccessChannel(AccessChannel.BOTH);

        admin.setPermissions(PermissionTemplate.defaultPermissions(
                UserType.PINETECHS_SUPPORT_STAFF,
                null,
                null
        ));

        admin.setInventoryDomains(PermissionTemplate.defaultDomains(
                UserType.PINETECHS_SUPPORT_STAFF,
                null
        ));

        admin.setEnabled(true);
        admin.setDeleted(false);
        admin.setCompanies(null);

        userRepository.save(admin);
    }

    private void createDefaultAdmin() {

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setEmail("admin@orvix.local");
        admin.setUserType(UserType.SYSTEM_ADMIN);
        admin.setAccessChannel(AccessChannel.WEB);

        admin.setPermissions(PermissionTemplate.defaultPermissions(
                UserType.SYSTEM_ADMIN,
                null,
                null
        ));

        admin.setInventoryDomains(PermissionTemplate.defaultDomains(
                UserType.SYSTEM_ADMIN,
                null
        ));

        admin.setEnabled(true);
        admin.setDeleted(false);
        admin.setCompanies(null);

        userRepository.save(admin);
    }

}
