package com.github;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 先向 eladmin 取 1 小时短票，失败则用本地未过期缓存。
 */
public class GitHubTokenProvider {

    static final String DEFAULT_TOKEN_URL = "https://www.mbtsp.com/api/plugin/github/token";
    static final String DEFAULT_ACCESS_KEY = "95f28a83f01fab551575b8d1d6b1f110d7bcf313";

    private static final long REFRESH_AHEAD_SECONDS = 120L;

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

    public synchronized String getToken() throws Exception {
        GitHubInstallationToken remote = fetchRemote();
        if (isUsable(remote)) {
            store.save(remote);
            return remote.getToken();
        }
        GitHubInstallationToken local = store.load();
        if (isUsable(local)) {
            return local.getToken();
        }
        throw new Exception("获取 GitHub 短票失败，且本地没有可用缓存");
    }

    private GitHubInstallationToken fetchRemote() {
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("User-Agent", "issueAppRoot");
            heads.put("X-Plugin-Key", accessKey);
            HttpClient httpClient = new HttpClient(heads, 10000);
            HttpClientResponse<GitHubInstallationToken> response =
                    httpClient.get(tokenUrl, null, GitHubInstallationToken.class);
            if (response.getCode() == null || response.getCode() / 100 != 2) {
                return null;
            }
            return response.getBody();
        } catch (Exception e) {
            return null;
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
}
