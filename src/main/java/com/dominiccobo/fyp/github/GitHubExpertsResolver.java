package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.context.api.queries.AssociatedExpertsQuery;
import com.dominiccobo.fyp.context.listeners.ExpertsQueryListener;
import com.dominiccobo.fyp.context.models.*;
import com.dominiccobo.fyp.context.models.git.GitContext;
import com.dominiccobo.fyp.context.models.git.GitRemoteIdentifier;
import com.dominiccobo.fyp.context.models.git.GitRemoteURL;
import com.dominiccobo.fyp.github.provider.Collaborator;
import com.dominiccobo.fyp.github.provider.GitHubAPI;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GitHubExpertsResolver implements ExpertsQueryListener {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubExpertsResolver.class);

    private final GitHubAPI api;

    @Autowired
    public GitHubExpertsResolver(GitHubAPI api) {
        this.api = api;
    }

    @QueryHandler
    @Override
    public List<Expert> on(AssociatedExpertsQuery query) {
        LOG.info("Received query for associated work items");
        QueryContext queryContext = query.getContext();
        Pagination pagination = query.getPagination();
        return fetchExpertsGivenSufficientGitContext(queryContext, pagination).collect(Collectors.toList());
    }

    private Stream<Expert> fetchExpertsGivenSufficientGitContext(QueryContext context, Pagination pagination) {
        if (!context.getGitContext().isPresent()) {
            return Stream.empty();
        }
        else {
            GitContext gitContext = context.getGitContext().get();
            return fetchExpertsForRemotes(gitContext, pagination).map(this::transformCollaboratorToExpert);
        }
    }

    private Expert transformCollaboratorToExpert(Collaborator collaborator) {
        return new Expert.Builder()
                .setExpertName(collaborator.getName())
                .withContactDetails(new ContactDetails("Email", collaborator.getContactEmail()))
                .withContactDetails(new ContactDetails("Profile", collaborator.getProfileLink()))
                .withExpertiseOn(new ExpertTopic("Collaborator", "Contributes to someWayOfGettingRepoInHereWeProbablyNeedAnInterface"))
                .build();
    }

    private Stream<Collaborator> fetchExpertsForRemotes(GitContext gitContext, Pagination pagination) {
        if (!gitContext.getRemotes().isPresent()) {
            return Stream.empty();
        }
        else {
            Map<GitRemoteIdentifier, GitRemoteURL> remotes = gitContext.getRemotes().get();
            return remotes.entrySet().stream().flatMap(gitRemoteURL -> fetchExpertsForRemote(gitRemoteURL, pagination));
        }
    }

    private Stream<Collaborator> fetchExpertsForRemote(Map.Entry<GitRemoteIdentifier, GitRemoteURL> remote, Pagination pagination) {
        String remoteUrl = remote.getValue().getUrl();
        GitRepoDetails gitRepoDetails = GitRepoDetails.from(remoteUrl);

        if(gitRepoDetails == null) {
            return Stream.empty();
        }
        return api.getCollaborators(gitRepoDetails, pagination);
    }
}
