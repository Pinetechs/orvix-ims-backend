package com.pinetechs.orvix.ims.common.lookup.controller;

import com.pinetechs.orvix.ims.common.lookup.dto.LookupResponse;
import com.pinetechs.orvix.ims.common.lookup.service.LookupService;
import com.pinetechs.orvix.ims.common.service.Helper;
import com.pinetechs.orvix.ims.company.dto.CompanyResponse;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
public class LookupController {


    private final LookupService lookupService;
    private final Helper helper;


    public LookupController(LookupService lookupService, Helper helper) {
        this.lookupService = lookupService;
        this.helper = helper;
    }



    @GetMapping("/companies")
    public List<LookupResponse> getAllowedCompanies(@RequestParam(required = false) String search, @RequestParam(defaultValue = "20") int limit, Authentication authentication){
        User currentUser = helper.currentUser(authentication);

      return lookupService.searchAllowedCompanies(search,  currentUser,limit);
    }

    @GetMapping("/inventory-domains")
    public List<LookupResponse> getAllowedInventoryDomain(@RequestParam(required = false) String search, @RequestParam(defaultValue = "20") int limit, Authentication authentication){
        User currentUser = helper.currentUser(authentication);

      return lookupService.searchAllowedDomains(search,  currentUser,limit);
    }

    @GetMapping("/task-statuses")
    public List<LookupResponse> getTaskStatuses(@RequestParam(required = false) String search, @RequestParam(defaultValue = "20") int limit, Authentication authentication){
        User currentUser = helper.currentUser(authentication);

      return lookupService.searchTaskStatuses(search,  currentUser,limit);
    }


    @GetMapping("/user-types")
    public List<LookupResponse> getUserTypes(@RequestParam(required = false) String search, @RequestParam(defaultValue = "20") int limit, Authentication authentication){
        User currentUser = helper.currentUser(authentication);

      return lookupService.searchUserTypes(search,  currentUser,limit);
    }


}
