package com.armzofficial.fantasycore.item;

import java.util.Objects;
import java.util.UUID;

/** หลักฐาน Core ต้องตรงกับทะเบียน; vanilla ไม่มี serial/owner ใน Core */
public final class ItemIdentityPolicy {
    public record Identity(String provider, String template, int version, UUID serial) {
        public Identity {
            if (!Objects.equals(provider, "vanilla") && !Objects.equals(provider, "core")) {
                throw new IllegalArgumentException("unsupported item provider");
            }
            if (provider.equals("core") && (template == null || !template.matches("[a-z0-9_]{1,48}") || version < 1 || serial == null)) {
                throw new IllegalArgumentException("invalid core identity");
            }
            if (provider.equals("vanilla") && (template != null || version != 0 || serial != null)) {
                throw new IllegalArgumentException("vanilla cannot have core identity");
            }
        }
    }

    private ItemIdentityPolicy() {
    }

    public static boolean registeredFor(Identity identity, UUID holder, ItemInstanceStore.Instance instance) {
        if (identity.provider().equals("vanilla")) {
            return instance == null;
        }
        return instance != null && identity.serial().equals(instance.serial())
                && identity.template().equals(instance.templateId()) && identity.version() == instance.version()
                && holder.equals(instance.owner())
                && (instance.state().equals("DELIVERED") || instance.state().equals("MAILED"));
    }
}
