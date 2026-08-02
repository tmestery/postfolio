package com.postfolio.postfolio.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads a repo-root (or nearby) {@code .env} into JVM system properties before
 * Spring starts, so {@code GROQ_API_KEY} and datasource vars work without
 * exporting them in the shell.
 *
 * Existing environment variables always win over {@code .env} values.
 */
public final class DotenvBootstrap {

    private DotenvBootstrap() {}

    public static void load() {
        Path envFile = findEnvFile();
        if (envFile == null) {
            return;
        }
        Dotenv dotenv = Dotenv.configure()
                .directory(envFile.getParent().toString())
                .filename(envFile.getFileName().toString())
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            if (System.getenv(key) != null) {
                return; // shell / docker env wins
            }
            if (System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        });
    }

    /** Walks common locations when the process is started from Backend/postfolio. */
    static Path findEnvFile() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                cwd.resolve(".env"),
                cwd.resolve("../.env"),
                cwd.resolve("../../.env"),
                cwd.getParent() != null ? cwd.getParent().resolve(".env") : null,
        };
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate.normalize();
            }
        }
        return null;
    }
}
