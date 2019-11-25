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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class GitHubExpertsResolutionTest {

    private ExpertsQueryListener fixture;
    @Mock
    private GitHubAPI api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fixture = new GitHubExpertsResolver(api);
    }

    @Test
    public void whenAssociatedExpertsQueriedAndRemoteIsNotGitHub_emptyResultsReturned() {
        AssociatedExpertsQuery qry = new AssociatedExpertsQuery(buildNonGitHubQueryContext());
        List<Expert> result = fixture.on(qry);
        assertThat(result).isEmpty();
    }

    @Test
    public void testGivenRepoExistsWithOneExperts_whenAssociatedExpertsQueried_singleExpertIsReturned() {
        String CONTACT_MODE = "GitHub Profile URL";
        String GITHUB_PROFILE = "https://github.com/dominiccobo";
        String EXPERT_NAME = "Dominic Cobo";
        String MY_TOPIC = "Repository Contributor";
        String MY_DESCRIPTION = "Has contributed towards repository";
        String EMAIL_MODE  = "Email";
        String EMAIL_ADDRESS = "sample@example.com";

        when(api.getCollaborators(any())).thenReturn(Collections.singletonList(
                new SampleCollaborator(EMAIL_ADDRESS, GITHUB_PROFILE, EXPERT_NAME)
        ));

        Expert expectedExpert = new Expert.Builder()
                .setExpertName(EXPERT_NAME)
                .withExpertiseOn(new SampleExpertTopic(MY_TOPIC, MY_DESCRIPTION))
                .withContactDetails(new SampleContactDetails(CONTACT_MODE, GITHUB_PROFILE))
                .withContactDetails(new SampleContactDetails(EMAIL_MODE, EMAIL_ADDRESS))
                .build();

        AssociatedExpertsQuery qry = new AssociatedExpertsQuery(buildGitHubQueryContext());
        List<Expert> result = fixture.on(qry);
        assertThat(result).isNotEmpty();
        assertThat(result).contains(expectedExpert);
    }

    private QueryContext buildNonGitHubQueryContext() {
        Map<GitRemoteIdentifier, GitRemoteURL> remotes = new HashMap<>();
        remotes.put(new GitRemoteIdentifier("upstream"), new GitRemoteURL("git@github.com:dominiccobo/cs3004-assignment.git"));
        GitContext gitContext = new GitContext(remotes, null);
        return new QueryContext(gitContext, null);
    }

    private QueryContext buildGitHubQueryContext() {
        Map<GitRemoteIdentifier, GitRemoteURL> remotes = new HashMap<>();
        remotes.put(new GitRemoteIdentifier("upstream"), new GitRemoteURL("git@github.com:dominiccobo/cs3004-assignment.git"));
        GitContext gitContext = new GitContext(remotes, null);
        return new QueryContext(gitContext, null);
    }

    private static class SampleExpertTopic extends ExpertTopic {

        public SampleExpertTopic(String topicName, String description) {
            super(topicName, description);
        }
    }

    private static class SampleContactDetails extends ContactDetails {

        public SampleContactDetails(String meansName, String details) {
            super(meansName, details);
        }
    }

    private static class SampleCollaborator implements Collaborator {

        private final String email;
        private final String profileLink;
        private final String name;

        private SampleCollaborator(String email, String profileLink, String name) {
            this.email = email;
            this.profileLink = profileLink;
            this.name = name;
        }

        @Override
        public String getContactEmail() {
            return this.email;
        }

        @Override
        public String getProfileLink() {
            return this.profileLink;
        }

        @Override
        public String getName() {
            return this.name;
        }

    }
}
