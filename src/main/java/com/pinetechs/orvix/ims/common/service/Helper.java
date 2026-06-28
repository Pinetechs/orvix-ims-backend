package com.pinetechs.orvix.ims.common.service;

import com.pinetechs.orvix.ims.security.JwtUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class Helper {



    public com.pinetechs.orvix.ims.user.entity.User currentUser(Authentication authentication) {
        return ((JwtUserDetails) authentication.getPrincipal()).getUser();
    }


}
