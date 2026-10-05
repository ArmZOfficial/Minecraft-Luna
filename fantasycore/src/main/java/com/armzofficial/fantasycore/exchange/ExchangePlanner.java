package com.armzofficial.fantasycore.exchange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** วางแผนตัดเฉพาะ storage slots ที่เป็น vanilla ไม่มี metadata; ยังไม่แก้ inventory */
public final class ExchangePlanner {
    public record Input(String material, int amount) {
        public Input {
            if (material == null || material.isBlank() || amount < 1 || amount > 2304) {
                throw new IllegalArgumentException("invalid input");
            }
        }
    }

    public record Slot(int index, String material, int amount, boolean plain) {
    }

    public record Take(int slot, int amount) {
    }

    private ExchangePlanner() {
    }

    public static Optional<List<Take>> plan(List<Input> inputs, int batch, List<Slot> slots) {
        if (batch < 1 || batch > 16 || inputs.isEmpty()) {
            throw new IllegalArgumentException("invalid recipe or batch");
        }
        Set<String> materials = new HashSet<>();
        Set<Integer> indexes = new HashSet<>();
        for (Slot slot : slots) {
            if (slot.index() < 0 || slot.index() >= 36 || slot.amount() < 1 || !indexes.add(slot.index())) {
                throw new IllegalArgumentException("invalid or duplicate storage slot");
            }
        }
        List<Take> result = new ArrayList<>();
        for (Input input : inputs) {
            if (!materials.add(input.material())) {
                throw new IllegalArgumentException("duplicate material");
            }
            int remaining = Math.multiplyExact(input.amount(), batch);
            for (Slot slot : slots) {
                if (slot.plain() && slot.material().equals(input.material())) {
                    int count = Math.min(remaining, slot.amount());
                    if (count > 0) {
                        result.add(new Take(slot.index(), count));
                        remaining -= count;
                    }
                }
            }
            if (remaining != 0) {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(result));
    }
}
