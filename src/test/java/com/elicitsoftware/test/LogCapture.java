package com.elicitsoftware.test;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.jboss.logmanager.ExtLogRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Collects what {@code com.elicitsoftware} logs while a test runs, at every level, so a test
 * can assert on what a log record carries (NFR-009). Use in a try-with-resources block; closing
 * it detaches the handler and puts the logger's level back.
 */
public final class LogCapture extends Handler implements AutoCloseable {

    private static final String CATEGORY = "com.elicitsoftware";

    private final Logger logger = Logger.getLogger(CATEGORY);
    private final Level previousLevel = logger.getLevel();
    private final List<String> messages = new ArrayList<>();
    private final SimpleFormatter formatter = new SimpleFormatter();

    private LogCapture() {
        setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.addHandler(this);
    }

    /** Starts capturing. */
    public static LogCapture start() {
        return new LogCapture();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        StringBuilder line = new StringBuilder();
        line.append(record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : formatter.formatMessage(record));
        // A logged exception prints its message and its causes' messages with the stack trace.
        for (Throwable t = record.getThrown(); t != null; t = t.getCause()) {
            line.append(" | ").append(t);
        }
        messages.add(line.toString());
    }

    /** Every captured record: the formatted message, then any exception chain. */
    public synchronized List<String> messages() {
        return List.copyOf(messages);
    }

    /** All captured records as one text, one per line. */
    public synchronized String text() {
        return String.join(System.lineSeparator(), messages);
    }

    /** Whether any captured record contains the text. */
    public boolean contains(String text) {
        return text().contains(text);
    }

    @Override
    public void flush() {
        // nothing is buffered
    }

    @Override
    public void close() {
        logger.removeHandler(this);
        logger.setLevel(previousLevel);
    }
}
