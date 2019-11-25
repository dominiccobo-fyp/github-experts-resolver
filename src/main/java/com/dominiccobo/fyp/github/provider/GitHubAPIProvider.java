package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import org.kohsuke.github.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GitHubAPIProvider implements GitHubAPI {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAPIProvider.class);

    private final GitHub apiProvider;

    @Autowired
    public GitHubAPIProvider(GitHub apiProvider) {
        this.apiProvider = apiProvider;
    }

    @Override
    public List<Collaborator> getCollaborators(GitRepoDetails repoDetails) {
        return mapToApiSafeInterface(getRepositoryCollaborators(repoDetails));
    }

    private List<Collaborator> mapToApiSafeInterface(GHPersonSet<GHUser> repositoryCollaborators) {
        return repositoryCollaborators.stream().map(BoundarySafeApiUser::new).collect(Collectors.toList());
    }

    private GHPersonSet<GHUser> getRepositoryCollaborators(GitRepoDetails repoDetails) {
        String userRepoString = String.format("%s/%s", repoDetails.getUserOrOrganisation(), repoDetails.getRepositoryName());
        try {
           return apiProvider.getRepository(userRepoString).getCollaborators();
        } catch (IOException e) {
            LOG.error("Could not retrieve repository or collaborators. Empty result set returned. Reason was: ", e);
            return new GHPersonSet<>();
        }
    }

    private static class BoundarySafeApiUser implements Collaborator {

        private final GHUser ghUser;

        private BoundarySafeApiUser(GHUser ghUser) {
            this.ghUser = ghUser;
        }

        @Override
        public String getContactEmail() {
            try {
                return ghUser.getEmail();
            } catch (IOException e) {
                LOG.error("Could not retrieve email. ", e);
                return null;
            }
        }

        @Override
        public String getProfileLink() {
            return ghUser.getHtmlUrl().toString();
        }

        @Override
        public String getName() {
            try {
                return ghUser.getName();
            } catch (IOException e) {
                LOG.error("Could not retrieve user name. ", e);
                return null;
            }
        }
    }
}
