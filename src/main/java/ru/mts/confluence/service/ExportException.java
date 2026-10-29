package ru.mts.confluence.service;

public class ExportException extends Exception {
    private final int httpStatus;
    private final String i18nKey;
    private final Object[] args;

    public ExportException(int httpStatus, String i18nKey, Object... args) {
        super(i18nKey);
        this.httpStatus = httpStatus;
        this.i18nKey = i18nKey;
        this.args = args;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
