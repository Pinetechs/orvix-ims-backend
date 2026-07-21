package com.pinetechs.orvix.ims.inventory.tracking.provider;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class InventoryTrackingProviderRegistry {

    private final Map<InventoryDomain, InventoryTrackingProvider> providers;

    public InventoryTrackingProviderRegistry(List<InventoryTrackingProvider> providerList) {
        Map<InventoryDomain, InventoryTrackingProvider> registered = new EnumMap<>(InventoryDomain.class);
        for (InventoryTrackingProvider provider : providerList) {
            InventoryTrackingProvider previous = registered.put(provider.domain(), provider);
            if (previous != null) {
                throw new IllegalStateException("Duplicate tracking provider for domain " + provider.domain());
            }
        }
        for (InventoryDomain domain : InventoryDomain.values()) {
            if (!registered.containsKey(domain)) {
                throw new IllegalStateException("Missing tracking provider for domain " + domain);
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public InventoryTrackingProvider get(InventoryDomain domain) {
        InventoryTrackingProvider provider = providers.get(domain);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported inventory domain: " + domain);
        }
        return provider;
    }
}
