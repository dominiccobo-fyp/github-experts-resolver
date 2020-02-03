package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.context.models.Pagination;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class GitHubAPIProvider implements GitHubAPI {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAPIProvider.class);

    private final RestTemplate restTemplate;
    private Map<String, ArrayList<GitHubUser>> cache = new HashMap<>();

    @Autowired
    public GitHubAPIProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Stream<Collaborator> getCollaborators(GitRepoDetails repoDetails, Pagination pagination) {
        return fetchCollaboratorsForRemoteRepository(repoDetails, pagination).stream().map(BoundarySafeApiUser::new);
    }

    private static class BoundarySafeApiUser implements Collaborator {

        private final GitHubUser ghUser;

        private BoundarySafeApiUser(GitHubUser ghUser) {
            this.ghUser = ghUser;
        }

        @Override
        public String getContactEmail() {
            return ghUser.getEmail();
        }

        @Override
        public String getProfileLink() {
            return ghUser.getUrl();
        }

        @Override
        public String getName() {
            return ghUser.getName();
        }
    }

    private List<GitHubUser> fetchCollaboratorsForRemoteRepository(GitRepoDetails gitRepoDetails, Pagination pagination) {
        String url = "/repos/{owner}/{repo}/collaborators?page={page}&per_page={size}&affiliation=all";
        Map<String, String> uriVars = new HashMap<>();
        uriVars.put("page", String.valueOf(pagination.page));
        uriVars.put("size", String.valueOf(pagination.itemsPerPage));
        uriVars.put("owner", gitRepoDetails.getUserOrOrganisation());
        uriVars.put("repo", gitRepoDetails.getRepositoryName());

        ResponseEntity<GitHubUser[]> forEntity = this.restTemplate.getForEntity(url, GitHubUser[].class, uriVars);

        if(forEntity.getStatusCode().is3xxRedirection()) {
            LOG.info("Fetching from short term cache");
            return cache.get(getCacheKey(gitRepoDetails));
        }

        if(forEntity.getBody() != null) {
            return getEnrichedUserData(gitRepoDetails, forEntity);
        }

        return new ArrayList<>();
    }

    private List<GitHubUser> getEnrichedUserData(GitRepoDetails gitRepoDetails, ResponseEntity<GitHubUser[]> forEntity) {
        GitHubUser[] retrievedEntities = forEntity.getBody();
        List<GitHubUser> gitHubUsers = new ArrayList<>();
        for (GitHubUser retrievedEntity : retrievedEntities) {
            String userUrl = String.format("/users/%s", retrievedEntity.getLogin());
            ResponseEntity<GitHubUser> moreInformedEntity = this.restTemplate.getForEntity(userUrl, GitHubUser.class);
            if (moreInformedEntity.getBody() != null) {
                gitHubUsers.add(moreInformedEntity.getBody());
            }
        }
        addToCache(gitRepoDetails, gitHubUsers);
        return gitHubUsers;
    }

    private void addToCache(GitRepoDetails gitRepoDetails, List<GitHubUser> gitIssues) {
        String key = getCacheKey(gitRepoDetails);
        ArrayList<GitHubUser> orDefault = cache.getOrDefault(key, new ArrayList<>());
        orDefault.addAll(gitIssues);
        cache.put(key, orDefault);
    }

    private String getCacheKey(GitRepoDetails gitRepoDetails) {
        return gitRepoDetails.getUserOrOrganisation() + gitRepoDetails.getRepositoryName();
    }

    private static class GitHubUser {

        private GitHubUser() {}

        private String login;
        private String id;
        @JsonProperty("avatar_url")
        private String avatarUrl;
        private String url;
        @JsonProperty(required = false)
        private String email;
        @JsonProperty(required = false)
        private String name;

        public String getLogin() {
            return login;
        }

        public String getId() {
            return id;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public String getUrl() {
            return url;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }
    }
}
