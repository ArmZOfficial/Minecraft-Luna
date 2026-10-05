package com.armzofficial.fantasycore.exchange;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExchangePlannerTest {
    @Test
    void splitsAcrossStacksAndLeavesUnrelatedSlotsAlone() {
        var result = ExchangePlanner.plan(List.of(new ExchangePlanner.Input("WHEAT", 32)), 2, List.of(
                new ExchangePlanner.Slot(0, "WHEAT", 40, true),
                new ExchangePlanner.Slot(1, "DIAMOND", 64, true),
                new ExchangePlanner.Slot(5, "WHEAT", 32, true))).orElseThrow();
        assertEquals(List.of(new ExchangePlanner.Take(0, 40), new ExchangePlanner.Take(5, 24)), result);
    }

    @Test
    void customItemsAreNeverIngredientsEvenWhenMaterialMatches() {
        var inputs = List.of(new ExchangePlanner.Input("IRON_INGOT", 4));
        assertTrue(ExchangePlanner.plan(inputs, 1, List.of(new ExchangePlanner.Slot(0, "IRON_INGOT", 64, false))).isEmpty());
        assertEquals(List.of(new ExchangePlanner.Take(2, 4)), ExchangePlanner.plan(inputs, 1, List.of(
                new ExchangePlanner.Slot(0, "IRON_INGOT", 64, false),
                new ExchangePlanner.Slot(2, "IRON_INGOT", 4, true))).orElseThrow());
    }

    @Test
    void noPartialPlanWhenOneIngredientIsMissing() {
        assertTrue(ExchangePlanner.plan(List.of(new ExchangePlanner.Input("IRON_INGOT", 6),
                new ExchangePlanner.Input("STICK", 4)), 1,
                List.of(new ExchangePlanner.Slot(0, "IRON_INGOT", 6, true))).isEmpty());
    }

    @Test
    void batchBoundsAndSlotBoundsAreEnforced() {
        var inputs = List.of(new ExchangePlanner.Input("WHEAT", 1));
        var slots = List.of(new ExchangePlanner.Slot(0, "WHEAT", 64, true));
        assertThrows(IllegalArgumentException.class, () -> ExchangePlanner.plan(inputs, 0, slots));
        assertThrows(IllegalArgumentException.class, () -> ExchangePlanner.plan(inputs, 17, slots));
        assertEquals(List.of(new ExchangePlanner.Take(0, 16)), ExchangePlanner.plan(inputs, 16, slots).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> ExchangePlanner.plan(inputs, 1,
                List.of(new ExchangePlanner.Slot(36, "WHEAT", 64, true))));
    }

    @Test
    void duplicateMaterialsOrSlotsCannotDoubleCountInventory() {
        var input = new ExchangePlanner.Input("WHEAT", 1);
        var slot = new ExchangePlanner.Slot(0, "WHEAT", 64, true);
        assertThrows(IllegalArgumentException.class, () -> ExchangePlanner.plan(List.of(input, input), 1, List.of(slot)));
        assertThrows(IllegalArgumentException.class, () -> ExchangePlanner.plan(List.of(input), 1, List.of(slot, slot)));
    }
}
