package com.dominiccobo.fyp.github;

import org.junit.*;
import org.kohsuke.github.*;

import java.io.IOException;
import java.net.URL;

public class GitHubContributorsAPIIntegrationTest {

    private GitHub fixture;

    @Before
    public void setUp() throws IOException {
        fixture = GitHub.connectUsingOAuth(System.getenv("GITHUB_OAUTH_KEY"));
    }

    @Test
    public void testRetrieveCollaborators() throws IOException {
        GHPersonSet<GHUser> collaborators = fixture
                .getRepository("dominiccobo/cs3004-assignment")
                .getCollaborators();

        for (GHUser collaborator : collaborators) {
            String email = collaborator.getEmail();
            URL pageUrl = collaborator.getHtmlUrl();
        }
    }
}
