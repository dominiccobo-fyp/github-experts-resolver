package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.github.utils.GitRepoDetails;

import java.util.List;

public interface GitHubAPI {
    List<Collaborator> getCollaborators(GitRepoDetails repoDetails);
}
