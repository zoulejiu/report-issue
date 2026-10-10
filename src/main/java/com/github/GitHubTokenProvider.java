package com.github;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 先向 eladmin 取 1 小时短票，失败则用本地未过期缓存。
 * <p>
 * 每次上报都会 new 一个 provider，所以短票按 tokenUrl 在进程内共享，并且取票串行：
 * 同时提交多份报告时只请求一次 token 服务，其余直接复用。
 */
public class GitHubTokenProvider {

    static final String DEFAULT_TOKEN_URL = "https://www.mbtsp.com/api/plugin/github/token";
    static final String DEFAULT_ACCESS_KEY = "95f28a83f01fab551575b8d1d6b1f110d7bcf313";

    private static final long REFRESH_AHEAD_SECONDS = 120L;
    private static final int REMOTE_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MILLIS = 1000L;

    private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, GitHubInstallationToken> CACHE = new ConcurrentHashMap<>();

    private final String tokenUrl;
    private final String accessKey;
    private final GitHubTokenStore store;

    public GitHubTokenProvider() {
        this(envOr("REPORT_ISSUE_TOKEN_URL", DEFAULT_TOKEN_URL),
                envOr("REPORT_ISSUE_PLUGIN_KEY", DEFAULT_ACCESS_KEY),
                new GitHubTokenStore());
    }

    GitHubTokenProvider(String tokenUrl, String accessKey, GitHubTokenStore store) {
        this.tokenUrl = tokenUrl;
        this.accessKey = accessKey;
        this.store = store;
    }

    public String getToken() throws Exception {
        GitHubInstallationToken cached = CACHE.get(tokenUrl);
        if (isUsable(cached)) {
            return cached.getToken();
        }
        synchronized (LOCKS.computeIfAbsent(tokenUrl, k -> new Object())) {
            cached = CACHE.get(tokenUrl);
            if (isUsable(cached)) {
                return cached.getToken();
            }
            String failure = null;
            for (int attempt = 1; attempt <= REMOTE_ATTEMPTS; attempt++) {
                RemoteResult remote = fetchRemote();
                if (isUsable(remote.token)) {
                    CACHE.put(tokenUrl, remote.token);
                    store.save(remote.token);
                    return remote.token.getToken();
                }
                failure = remote.failure;
                if (attempt < REMOTE_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MILLIS);
                }
            }
            GitHubInstallationToken local = store.load();
            if (isUsable(local)) {
                CACHE.put(tokenUrl, local);
                return local.getToken();
            }
            throw new Exception("Could not get a GitHub access token (" + failure + ") and no valid cached token is available");
        }
    }

    private RemoteResult fetchRemote() {
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("User-Agent", "issueAppRoot");
            heads.put("X-Plugin-Key", accessKey);
            HttpClient httpClient = new HttpClient(heads, 10000);
            HttpClientResponse<GitHubInstallationToken> response =
                    httpClient.get(tokenUrl, null, GitHubInstallationToken.class);
            if (response.getCode() == null || response.getCode() / 100 != 2) {
                return RemoteResult.failed("HTTP " + response.getCode());
            }
            GitHubInstallationToken token = response.getBody();
            if (token == null || StringUtils.isBlank(token.getToken())) {
                return RemoteResult.failed("no token in response");
            }
            return RemoteResult.ok(token);
        } catch (Exception e) {
            return RemoteResult.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private boolean isUsable(GitHubInstallationToken token) {
        if (token == null || StringUtils.isBlank(token.getToken())) {
            return false;
        }
        if (StringUtils.isBlank(token.getExpiresAt())) {
            return true;
        }
        try {
            Instant expires = Instant.parse(token.getExpiresAt());
            return Instant.now().plusSeconds(REFRESH_AHEAD_SECONDS).isBefore(expires);
        } catch (Exception e) {
            return true;
        }
    }

    private static String envOr(String name, String defaultValue) {
        String value = System.getenv(name);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    private static final class RemoteResult {
        final GitHubInstallationToken token;
        final String failure;

        private RemoteResult(GitHubInstallationToken token, String failure) {
            this.token = token;
            this.failure = failure;
        }

        static RemoteResult ok(GitHubInstallationToken token) {
            return new RemoteResult(token, null);
        }

        static RemoteResult failed(String failure) {
            return new RemoteResult(null, failure);
        }
    }
}
