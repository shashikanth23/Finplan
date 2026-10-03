package com.finplan.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Keyword rules for bank/UPI narrations. First matching category wins; anything else is OTHER. */
public final class ExpenseCategorizer {

    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();
    static {
        RULES.put("RENT", List.of("rent", "landlord", "nobroker", "housing society", "maintenance"));
        RULES.put("INSURANCE", List.of("insurance", "lic ", "premium", "policybazaar", "hdfc life", "star health"));
        RULES.put("EDUCATION", List.of("tuition", "school", "college", "course", "udemy", "coursera", "exam fee"));
        RULES.put("UTILITIES", List.of("electricity", "bescom", "tsspdcl", "water bill", "gas", "broadband", "airtel",
                "jio", "recharge", "wifi", "act fibernet"));
        RULES.put("TRANSPORT", List.of("uber", "ola", "rapido", "petrol", "fuel", "metro", "irctc", "redbus", "fastag"));
        RULES.put("FOOD", List.of("swiggy", "zomato", "restaurant", "cafe", "grocer", "bigbasket", "blinkit",
                "zepto", "supermarket", "dmart"));
        RULES.put("LIFESTYLE", List.of("netflix", "amazon", "flipkart", "myntra", "movie", "bookmyshow", "gym",
                "spotify", "hotstar", "prime video"));
    }

    private ExpenseCategorizer() {}

    public static String categorize(String description) {
        if (description == null) return "OTHER";
        String d = description.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
            for (String keyword : rule.getValue()) {
                if (d.contains(keyword)) return rule.getKey();
            }
        }
        return "OTHER";
    }
}
