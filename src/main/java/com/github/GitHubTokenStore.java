package com.github;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 把短票缓存在用户目录，token 服务短暂不可用时还能提交 issue。
 */
class GitHubTokenStore {

    private static final String FILE_NAME = "github-token.json";
    private final Gson gson = new Gson();
    private final Path file;

    GitHubTokenStore() {
        this(Paths.get(System.getProperty("user.home"), ".report-issue", FILE_NAME));
    }

    GitHubTokenStore(Path file) {
        this.file = file;
    }

    GitHubInstallationToken load() {
        try {
            if (!Files.exists(file)) {
                return null;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(json)) {
                return null;
            }
            return gson.fromJson(json, GitHubInstallationToken.class);
        } catch (Exception e) {
            return null;
        }
    }

    void save(GitHubInstallationToken token) {
        if (token == null || StringUtils.isBlank(token.getToken())) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Write then move, so a concurrent load() never reads a half-written file.
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, gson.toJson(token), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }
}
