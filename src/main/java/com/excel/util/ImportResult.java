package com.excel.util;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int insertedCount;
    private int updatedCount;
    private List<String> errorMessages;

    public ImportResult() {
        this.insertedCount = 0;
        this.updatedCount = 0;
        this.errorMessages = new ArrayList<>();
    }

    public void incrementInserted() {
        insertedCount++;
    }

    public void incrementUpdated() {
        updatedCount++;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public int getTotalProcessed() {
        return insertedCount + updatedCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
}