package nl.wilcokas.luckystackworker.service.dto;

import lombok.Getter;

@Getter
public class GithubReleaseDto {
    private String url;
    private String assetsUrl;
    private String uploadUrl;
    private String htmlUrl;
    private long id;
    private String nodeId;
    private String tagName;
    private String targetCommitish;
    private String name;
    private boolean draft;
    private boolean prerelease;
    private String createdAt;
    private String publishedAt;
    private String tarballUrl;
    private String zipballUrl;
    private String body;
}
