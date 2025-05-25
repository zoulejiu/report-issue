package com.github;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubDeviceAuthApis {
    private final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";
    public static String token = "11AHTYGQY0LRvVWWRyfsR0_emdEdrpcfVTEfXqpVYD3d4BLEP6Ra98W1VjLaFFIbvZR5ZIASMTEDZTu3FV";

    Map<String, String> heads = new HashMap<>();

    {
        heads.put("User-Agent", "issueAppRoot");
        heads.put("X-GitHub-Api-Version", "2022-11-28");
        heads.put("Authorization", String.format("Bearer github_pat_%s", token));
    }

    GitHubDeviceCode getDeviceCode(String clientId) throws Exception {
        HttpClient httpClient = new HttpClient();
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        HttpClientResponse<GitHubDeviceCode> httpClientResponse = httpClient.post(DEVICE_CODE_URL, params, GitHubDeviceCode.class);
        if (httpClientResponse.isStatus()) {
            return httpClientResponse.getBody();
        }
        return null;
    }

    public IssueInfo.ItemsDTO findIssue(String repo,String id) throws Exception {
        String url = "https://api.github.com/search/issues";
        Map<String, String> params = new HashMap<>();
        params.put("q", "repo:"+repo+" is:issue in:body " + id);
        params.put("page", "1");
        params.put("per_page", "per_page");
        HttpClient httpClient = new HttpClient(heads);
        HttpClientResponse<IssueInfo> httpClientResponse = httpClient.get(url, params, IssueInfo.class);
        if (httpClientResponse.isStatus()) {
            IssueInfo issueInfo = httpClientResponse.getBody();
            if (issueInfo != null && StringUtils.isNotBlank(issueInfo.getTotalCount())) {
                List<IssueInfo.ItemsDTO> itemsDTOS = issueInfo.getItems();
                if (itemsDTOS != null && !itemsDTOS.isEmpty()) {
                    return itemsDTOS.get(0);
                }
            }
        }
        return null;

    }

    public Issue issue(String url,String title, String body) throws Exception {
        HttpClient httpClient = new HttpClient(heads);
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("body", body);
        List<String> labels = new ArrayList<>();
        labels.add("bug");
        map.put("labels", labels);
        HttpClientResponse<Issue> httpClientResponse = httpClient.post(url, map, Issue.class);
        if (httpClientResponse.isStatus()) {
            return httpClientResponse.getBody();
        }
        return null;

    }


}
