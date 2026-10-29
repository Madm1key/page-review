package ru.mts.confluence.gitlab;

public class GitLabException extends Exception {
    private final int status;

    public GitLabException(int status, String message) {
        super(message);
        this.status = status;
    }

    public GitLabException(String message, Throwable cause) {
        super(message, cause);
        this.status = -1;
    }

    /** HTTP status returned by GitLab, or -1 for connection problems. */
    public int getStatus() {
        return status;
    }
}
