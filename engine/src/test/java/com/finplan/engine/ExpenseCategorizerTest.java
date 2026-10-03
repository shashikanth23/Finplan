package com.finplan.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseCategorizerTest {
    @Test void mapsCommonNarrations() {
        assertEquals("FOOD", ExpenseCategorizer.categorize("UPI-SWIGGY-ORDER 4432"));
        assertEquals("TRANSPORT", ExpenseCategorizer.categorize("Uber India Systems"));
        assertEquals("RENT", ExpenseCategorizer.categorize("Rent for October"));
        assertEquals("OTHER", ExpenseCategorizer.categorize("UPI-RAMESH K"));
        assertEquals("OTHER", ExpenseCategorizer.categorize(null));
    }
}
