package com.accenture.franchise.domain.exception;

public enum ErrorCode {
    INVALID_ARGUMENT("ERR-400", "The provided argument does not satisfy domain constraints"),
    FRANCHISE_NOT_FOUND("ERR-404-FRANCHISE", "The requested franchise does not exist"),
    BRANCH_NOT_FOUND("ERR-404-BRANCH", "The requested branch does not exist for this franchise"),
    PRODUCT_NOT_FOUND("ERR-404-PRODUCT", "The requested product does not exist in this branch"),
    DUPLICATE_FRANCHISE_NAME("ERR-409-FRANCHISE", "A franchise with the specified name already exists"),
    DUPLICATE_BRANCH_NAME("ERR-409-BRANCH", "A branch with the specified name already exists in this franchise"),
    DUPLICATE_PRODUCT_NAME("ERR-409-PRODUCT", "A product with the specified name already exists in this branch");

    private final String code;
    private final String defaultTitle;

    ErrorCode(String code, String defaultTitle) {
        this.code = code;
        this.defaultTitle = defaultTitle;
    }

    public String code() {
        return code;
    }

    public String defaultTitle() {
        return defaultTitle;
    }
}
