package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.context.api.queries.AssociatedExpertsQuery;
import com.dominiccobo.fyp.context.listeners.ExpertsQueryListener;
import com.dominiccobo.fyp.context.models.ContactDetails;
import com.dominiccobo.fyp.context.models.Expert;
import com.dominiccobo.fyp.context.models.ExpertTopic;
import com.dominiccobo.fyp.context.models.QueryContext;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public List<Expert> on(AssociatedExpertsQuery associatedExpertsQuery) {
        QueryContext qryContext = associatedExpertsQuery.getContext();
        List<Expert> experts = fetchExpertsGivenSufficientGitContext(qryContext);
        LOG.info("Size was: {}", experts.size());
        return experts;
    }

    private List<Expert> fetchExpertsGivenSufficientGitContext(QueryContext context) {
        if (!context.getGitContext().isPresent()) {
            return new ArrayList<>();
        }
        else {
            GitContext gitContext = context.getGitContext().get();
            return transformCollaboratorsToExpert(fetchExpertsForRemotes(gitContext));
        }
    }

    private List<Expert> transformCollaboratorsToExpert(List<Collaborator> collaborators) {
        List<Expert> experts = new ArrayList<>();
        for (Collaborator collaborator : collaborators) {
            experts.add(transformCollaboratorToExpert(collaborator));
        }
        return experts;
    }

    private Expert transformCollaboratorToExpert(Collaborator collaborator) {
        return new Expert.Builder()
                .setExpertName(collaborator.getName())
                .withContactDetails(new ContactDetails("Email", collaborator.getContactEmail()))
                .withContactDetails(new ContactDetails("Profile", collaborator.getProfileLink()))
                .withExpertiseOn(new ExpertTopic("Collaborator", "Contributes to someWayOfGettingRepoInHereWeProbablyNeedAnInterface"))
                .build();
    }

    private List<Collaborator>  fetchExpertsForRemotes(GitContext gitContext) {
        if (!gitContext.getRemotes().isPresent()) {
            return new ArrayList<>();
        }
        else {
            List<Collaborator> collaborators = new ArrayList<>();
            Map<GitRemoteIdentifier, GitRemoteURL> remotes = gitContext.getRemotes().get();
            for (Map.Entry<GitRemoteIdentifier, GitRemoteURL> remote : remotes.entrySet()) {
                collaborators.addAll(fetchExpertsForRemote(remote));
            }
            return collaborators;
        }
    }

    private List<Collaborator> fetchExpertsForRemote(Map.Entry<GitRemoteIdentifier, GitRemoteURL> remote) {
        String remoteUrl = remote.getValue().getUrl();
        GitRepoDetails gitRepoDetails = GitRepoDetails.from(remoteUrl);

        if(gitRepoDetails == null) {
            return new ArrayList<>();
        }
        return api.getCollaborators(gitRepoDetails);
    }
}
