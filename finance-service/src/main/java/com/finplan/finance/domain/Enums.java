package com.finplan.finance.domain;

public final class Enums {
    private Enums() {}
    public enum IncomeType { BASIC, ALLOWANCE, BONUS, OTHER }
    public enum Frequency { MONTHLY, ANNUAL }
    public enum ExpenseCategory { RENT, FOOD, UTILITIES, TRANSPORT, INSURANCE, EDUCATION, LIFESTYLE, OTHER }
    public enum ExpenseKind { FIXED, VARIABLE }
    public enum LoanType { HOME, CAR, PERSONAL, EDUCATION, CREDIT_CARD, OTHER }
}
