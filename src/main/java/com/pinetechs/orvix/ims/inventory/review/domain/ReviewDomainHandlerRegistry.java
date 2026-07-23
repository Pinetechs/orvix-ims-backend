package com.pinetechs.orvix.ims.inventory.review.domain;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ReviewDomainHandlerRegistry {

    private final Map<InventoryDomain, ReviewDomainHandler> handlers;

    public ReviewDomainHandlerRegistry(List<ReviewDomainHandler> handlers) {
        Map<InventoryDomain, ReviewDomainHandler> byDomain = new EnumMap<>(InventoryDomain.class);
        for (ReviewDomainHandler handler : handlers) {
            ReviewDomainHandler previous = byDomain.put(handler.domain(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate review handler for " + handler.domain());
            }
        }
        this.handlers = Map.copyOf(byDomain);
    }

    public ReviewDomainHandler get(InventoryDomain domain) {
        ReviewDomainHandler handler = handlers.get(domain);
        if (handler == null) {
            throw new IllegalStateException("No review handler registered for " + domain);
        }
        return handler;
    }
}
