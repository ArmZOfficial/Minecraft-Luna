package com.armzofficial.fantasycore.item;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemIdentityPolicyTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID serial = UUID.randomUUID();
    private final ItemIdentityPolicy.Identity core = new ItemIdentityPolicy.Identity("core", "starter_runeblade", 1, serial);

    private ItemInstanceStore.Instance instance(UUID id, String template, int version, UUID holder, String state) {
        return new ItemInstanceStore.Instance(id, template, version, holder, state, "test", 1000);
    }

    @Test
    void coreRequiresSerialTemplateVersionAndOwnerToMatchRegistry() {
        assertTrue(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "starter_runeblade", 1, owner, "DELIVERED")));
        assertFalse(ItemIdentityPolicy.registeredFor(core, owner, null));
        assertFalse(ItemIdentityPolicy.registeredFor(core, owner, instance(UUID.randomUUID(), "starter_runeblade", 1, owner, "DELIVERED")));
        assertFalse(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "other", 1, owner, "DELIVERED")));
        assertFalse(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "starter_runeblade", 2, owner, "DELIVERED")));
        assertFalse(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "starter_runeblade", 1, UUID.randomUUID(), "DELIVERED")));
        assertFalse(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "starter_runeblade", 1, null, "DELIVERED")));
    }

    @Test
    void onlyDeliveredOrMailedStatesAreEligible() {
        assertTrue(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "starter_runeblade", 1, owner, "MAILED")));
        for (String state : new String[]{"ISSUED", "DELIVERY_FAILED", "REVOKED", "REVIEW"}) {
            assertFalse(ItemIdentityPolicy.registeredFor(core, owner, instance(serial, "starter_runeblade", 1, owner, state)));
        }
    }

    @Test
    void unknownProvidersAndPartialCoreMarkersCannotBecomeVanilla() {
        assertThrows(IllegalArgumentException.class, () -> new ItemIdentityPolicy.Identity("mmoitems", null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ItemIdentityPolicy.Identity("core", "sword", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new ItemIdentityPolicy.Identity("vanilla", "sword", 1, serial));
        assertTrue(ItemIdentityPolicy.registeredFor(new ItemIdentityPolicy.Identity("vanilla", null, 0, null), owner, null));
    }
}
