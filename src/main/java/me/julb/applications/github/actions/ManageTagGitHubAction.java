/**
 * MIT License
 *
 * Copyright (c) 2017-2022 Julb
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.julb.applications.github.actions;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRef.GHObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import me.julb.sdk.github.actions.kit.GitHubActionsKit;
import me.julb.sdk.github.actions.spi.GitHubActionProvider;

/**
 * The action to manage tags. <br>
 * @author Julb.
 */
public class ManageTagGitHubAction implements GitHubActionProvider {

    /**
     * The GitHub action kit.
     */
    @Setter(AccessLevel.PACKAGE)
    private GitHubActionsKit ghActionsKit = GitHubActionsKit.INSTANCE;

    /**
     * The GitHub API.
     */
    @Setter(AccessLevel.PACKAGE)
    private GitHub ghApi;

    /**
     * The GitHub repository.
     */
    @Setter(AccessLevel.PACKAGE)
    private GHRepository ghRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            // Get inputs
            var tagName = getInputName();
            var tagState = getInputState();
            var from = getInputFrom();

            // Trace parameters
            ghActionsKit.debug(String.format("parameters: [name: %s, state: %s, from: %s]", tagName, tagState.name(), from));

            // Read GitHub repository.
            connectApi();

            // Retrieve repository
            ghRepository = ghApi.getRepository(ghActionsKit.getGitHubRepository());

            // Get existing tag if any.
            var existingTagGHRef = getTagGHRef(tagName);

            // Creation path.
            if (InputTagState.PRESENT.equals(tagState)) {
                // New ref
                var newRef = tagRef(tagName);

                // Get source SHA.
                var fromSha = getAnyGHRef(from).map(GHRef::getObject).map(GHObject::getSha).orElse(from);

                // Create tag.
                var ghRefCreated = createGHRef(newRef, fromSha, existingTagGHRef);

                // Set output.
                ghActionsKit.setOutput(OutputVars.REF.key(), ghRefCreated.getRef());
                ghActionsKit.setOutput(OutputVars.NAME.key(), tagName);
                ghActionsKit.setOutput(OutputVars.SHA.key(), ghRefCreated.getObject().getSha());
            } else {
                // Delete tag.
                deleteGHRef(existingTagGHRef);

                // Set empty output.
                ghActionsKit.setEmptyOutput(OutputVars.REF.key());
                ghActionsKit.setEmptyOutput(OutputVars.NAME.key());
                ghActionsKit.setEmptyOutput(OutputVars.SHA.key());
            }
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    // ------------------------------------------ Utility methods.

    /**
     * Gets the "name" input.
     * @return the "name" input.
     */
    String getInputName() {
        return ghActionsKit.getRequiredInput("name");
    }

    /**
     * Gets the "state" input.
     * @return the "state" input.
     */
    InputTagState getInputState() {
        return ghActionsKit.getEnumInput("state", InputTagState.class).orElse(InputTagState.PRESENT);
    }

    /**
     * Gets the "from" input.
     * @return the "from" input.
     */
    String getInputFrom() {
        return ghActionsKit.getInput("from").orElse(ghActionsKit.getGitHubSha());
    }

    /**
     * Connects to GitHub API.
     * @throws IOException if an error occurs.
     */
    void connectApi()
        throws IOException {
        ghActionsKit.debug("github api url connection: check.");

        // Get token
        var githubToken = ghActionsKit.getRequiredEnv("GITHUB_TOKEN");

        //@formatter:off
        ghApi = Optional.ofNullable(ghApi).orElse(new GitHubBuilder()
            .withEndpoint(ghActionsKit.getGitHubApiUrl())
            .withOAuthToken(githubToken)
            .build());
        ghApi.checkApiUrlValidity();
        ghActionsKit.debug("github api url connection: ok.");
        //@formatter:on
    }

    /**
     * Gets the {@link GHRef} tag matching the given name.
     * @param name the tag name to look for.
     * @return the {@link GHRef} for the given tag if exists, <code>false</code> otherwise.
     * @throws IOException if an error occurs.
     */
    Optional<GHRef> getTagGHRef(@NonNull String name)
        throws IOException {
        // Convert tag name to ref
        var tagRef = tagRef(name);

        // Browse existing refs
        try {
            for (GHRef ghRef : ghRepository.getRefs("tags")) {
                // Check if the tag to manage already exists.
                if (ghRef.getRef().equalsIgnoreCase(tagRef)) {
                    return Optional.of(ghRef);
                }
            }
        } catch (GHFileNotFoundException e) {
            ghActionsKit.debug("No tags found in this repository.");
        }

        return Optional.empty();
    }

    /**
     * Gets the {@link GHRef} branch or tag matching the given name.
     * @param name the branch or tag name to look for.
     * @return the {@link GHRef} for the given branch or tag if exists, <code>false</code> otherwise.
     * @throws IOException if an error occurs.
     */
    Optional<GHRef> getAnyGHRef(@NonNull String name)
        throws IOException {
        // Convert name to branch ref
        var branchRef = branchRef(name);

        // Convert name to tag ref
        var tagRef = tagRef(name);

        // List of candidates for which ref is OK.
        var candidates = List.of(branchRef.toLowerCase(), tagRef.toLowerCase(), name.toLowerCase());

        // Browse existing refs
        for (GHRef ghRef : ghRepository.getRefs()) {
            // Check if the ref is in the candidates.
            if (candidates.contains(ghRef.getRef().toLowerCase())) {
                return Optional.of(ghRef);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates or updates the {@link GHRef} if any.
     * @param newRef the ref to create.
     * @param sourceSHA the SHA from which to create the tag.
     * @param existingRef the {@link GHRef} for the existing tag, or {@link Optional#empty()} if the tag does not exist.
     * @return the {@link GHRef} created.
     * @throws IOException if an error occurs.
     */
    GHRef createGHRef(@NonNull String newRef, @NonNull String sourceSHA, @NonNull Optional<GHRef> existingRef)
        throws IOException {
        GHRef ghRefManaged;

        if (existingRef.isEmpty()) {
            // The tag does not exist: create
            ghActionsKit.notice("creating the ref.");
            ghRefManaged = ghRepository.createRef(newRef, sourceSHA);
        } else {
            // The tag already exists: update to source SHA.
            ghActionsKit.notice("updating the ref with the given SHA");
            ghRefManaged = existingRef.get();
            ghRefManaged.updateTo(sourceSHA, true);
        }

        return ghRefManaged;
    }

    /**
     * Deletes the {@link GHRef} if any.
     * @param refToDelete the {@link GHRef} to delete, or {@link Optional#empty()}.
     * @throws IOException if an error occurs.
     */
    void deleteGHRef(@NonNull Optional<GHRef> refToDelete)
        throws IOException {
        // Check if tag exists.
        if (refToDelete.isPresent()) {
            // The tag exists: delete.
            ghActionsKit.notice("deleting the tag.");
            refToDelete.get().delete();
        } else {
            // The tag does not exist, nothing to do.
            ghActionsKit.notice("skipping tag deletion as it does not exist.");
        }
    }

    /**
     * Gets the ref from a branch name.
     * @param branchName the branch name.
     * @return the ref for the given branch name.
     */
    String branchRef(@NonNull String branchName) {
        return String.format("refs/heads/%s", branchName);
    }

    /**
     * Gets the ref from a tag name.
     * @param name the tag name.
     * @return the ref for the given tag name.
     */
    String tagRef(@NonNull String name) {
        return String.format("refs/tags/%s", name);
    }
}
