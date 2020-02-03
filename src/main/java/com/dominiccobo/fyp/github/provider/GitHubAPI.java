package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.context.models.Pagination;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;

import java.util.stream.Stream;

public interface GitHubAPI {
    Stream<Collaborator> getCollaborators(GitRepoDetails repoDetails, Pagination pagination);
}
