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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRef.GHObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import me.julb.sdk.github.actions.kit.GitHubActionsKit;

/**
 * Test class for {@link ManageTagGitHubAction} class. <br>
 * @author Julb.
 */
@ExtendWith(MockitoExtension.class)
class ManageTagGitHubActionTest {

    /**
     * The class under test.
     */
    private ManageTagGitHubAction githubAction = null;

    /**
     * A mock for GitHub action kit.
     */
    @Mock
    private GitHubActionsKit ghActionsKitMock;

    /**
     * A mock for GitHub API.
     */
    @Mock
    private GitHub ghApiMock;

    /**
     * A mock for GitHub repository.
     */
    @Mock
    private GHRepository ghRepositoryMock;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        githubAction = new ManageTagGitHubAction();
        githubAction.setGhActionsKit(ghActionsKitMock);
        githubAction.setGhApi(ghApiMock);
        githubAction.setGhRepository(ghRepositoryMock);
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputName_thenReturnValue() throws Exception {
        when(this.ghActionsKitMock.getRequiredInput("name")).thenReturn("1.0.0");

        assertThat(this.githubAction.getInputName()).isEqualTo("1.0.0");

        verify(this.ghActionsKitMock).getRequiredInput("name");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputNameNotProvided_thenFail() {
        when(this.ghActionsKitMock.getRequiredInput("name")).thenThrow(NoSuchElementException.class);
        assertThrows(CompletionException.class, () -> this.githubAction.execute());
        verify(this.ghActionsKitMock).getRequiredInput("name");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputStateProvided_thenReturnValue() throws Exception {
        when(this.ghActionsKitMock.getEnumInput("state", InputTagState.class))
                .thenReturn(Optional.of(InputTagState.ABSENT));

        assertThat(this.githubAction.getInputState()).isEqualTo(InputTagState.ABSENT);

        verify(this.ghActionsKitMock).getEnumInput("state", InputTagState.class);
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputStateNotProvided_thenReturnDefaultValue() throws Exception {
        when(this.ghActionsKitMock.getEnumInput("state", InputTagState.class)).thenReturn(Optional.empty());

        assertThat(this.githubAction.getInputState()).isEqualTo(InputTagState.PRESENT);

        verify(this.ghActionsKitMock).getEnumInput("state", InputTagState.class);
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputFromProvided_thenReturnValue() throws Exception {
        when(this.ghActionsKitMock.getInput("from")).thenReturn(Optional.of("branch-name"));

        assertThat(this.githubAction.getInputFrom()).isEqualTo("branch-name");

        verify(this.ghActionsKitMock).getInput("from");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputFromNotProvided_thenReturnDefaultValue() throws Exception {
        when(this.ghActionsKitMock.getInput("from")).thenReturn(Optional.empty());
        when(this.ghActionsKitMock.getGitHubSha()).thenReturn("123456");

        assertThat(this.githubAction.getInputFrom()).isEqualTo("123456");

        verify(this.ghActionsKitMock).getInput("from");
        verify(this.ghActionsKitMock).getGitHubSha();
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteCreateTagNotExists_thenTagCreated() throws Exception {
        var spy = spy(this.githubAction);

        var ghRefCreated = Mockito.mock(GHRef.class);
        when(ghRefCreated.getRef()).thenReturn("refs/tags/1.0.0");
        var ghRefObject = Mockito.mock(GHObject.class);
        when(ghRefObject.getSha()).thenReturn("123456");
        when(ghRefCreated.getObject()).thenReturn(ghRefObject);

        when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
        doReturn("1.0.0").when(spy).getInputName();
        doReturn(InputTagState.PRESENT).when(spy).getInputState();
        doReturn("123456").when(spy).getInputFrom();

        doNothing().when(spy).connectApi();

        when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
        doReturn(Optional.empty()).when(spy).getTagGHRef("1.0.0");
        doReturn(Optional.empty()).when(spy).getAnyGHRef("123456");
        doReturn(ghRefCreated).when(spy).createGHRef("refs/tags/1.0.0", "123456", Optional.empty());

        spy.execute();

        verify(this.ghActionsKitMock).getGitHubRepository();

        verify(spy).getInputName();
        verify(spy).getInputState();
        verify(spy).getInputFrom();
        verify(spy).connectApi();
        verify(spy).getTagGHRef("1.0.0");
        verify(spy).getAnyGHRef("123456");
        verify(spy).createGHRef("refs/tags/1.0.0", "123456", Optional.empty());

        verify(this.ghApiMock).getRepository("octocat/Hello-World");
        verify(this.ghActionsKitMock).setOutput(OutputVars.REF.key(), "refs/tags/1.0.0");
        verify(this.ghActionsKitMock).setOutput(OutputVars.NAME.key(), "1.0.0");
        verify(this.ghActionsKitMock).setOutput(OutputVars.SHA.key(), "123456");
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteCreateTagAlreadyExists_thenTagUpdated() throws Exception {
        var spy = spy(this.githubAction);

        var ghRefCreated = Mockito.mock(GHRef.class);
        when(ghRefCreated.getRef()).thenReturn("refs/tags/1.0.0");
        var ghRefObject = Mockito.mock(GHObject.class);
        when(ghRefObject.getSha()).thenReturn("123456");
        when(ghRefCreated.getObject()).thenReturn(ghRefObject);

        var ghRefExisting = Mockito.mock(GHRef.class);

        when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
        doReturn("1.0.0").when(spy).getInputName();
        doReturn(InputTagState.PRESENT).when(spy).getInputState();
        doReturn("789123").when(spy).getInputFrom();

        doNothing().when(spy).connectApi();

        when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
        doReturn(Optional.of(ghRefExisting)).when(spy).getTagGHRef("1.0.0");
        doReturn(Optional.empty()).when(spy).getAnyGHRef("789123");
        doReturn(ghRefCreated).when(spy).createGHRef("refs/tags/1.0.0", "789123", Optional.of(ghRefExisting));

        spy.execute();

        verify(this.ghActionsKitMock).getGitHubRepository();

        verify(spy).getInputName();
        verify(spy).getInputState();
        verify(spy).getInputFrom();
        verify(spy).connectApi();
        verify(spy).getTagGHRef("1.0.0");
        verify(spy).getAnyGHRef("789123");
        verify(spy).createGHRef("refs/tags/1.0.0", "789123", Optional.of(ghRefExisting));

        verify(this.ghApiMock).getRepository("octocat/Hello-World");
        verify(this.ghActionsKitMock).setOutput(OutputVars.REF.key(), "refs/tags/1.0.0");
        verify(this.ghActionsKitMock).setOutput(OutputVars.NAME.key(), "1.0.0");
        verify(this.ghActionsKitMock).setOutput(OutputVars.SHA.key(), "123456");
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteDeleteTagAlreadyExists_thenTagDeleted() throws Exception {
        var spy = spy(this.githubAction);

        var ghRefExisting = Mockito.mock(GHRef.class);

        when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
        doReturn("1.0.0").when(spy).getInputName();
        doReturn(InputTagState.ABSENT).when(spy).getInputState();
        doReturn("789123").when(spy).getInputFrom();

        doNothing().when(spy).connectApi();

        when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
        doReturn(Optional.of(ghRefExisting)).when(spy).getTagGHRef("1.0.0");
        doNothing().when(spy).deleteGHRef(Optional.of(ghRefExisting));

        spy.execute();

        verify(this.ghActionsKitMock).getGitHubRepository();

        verify(spy).getInputName();
        verify(spy).getInputState();
        verify(spy).getInputFrom();
        verify(spy).connectApi();
        verify(spy).getTagGHRef("1.0.0");
        verify(spy).deleteGHRef(Optional.of(ghRefExisting));

        verify(this.ghApiMock).getRepository("octocat/Hello-World");
        verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.REF.key());
        verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.NAME.key());
        verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.SHA.key());
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteDeleteTagNotExists_thenTagDeleted() throws Exception {
        var spy = spy(this.githubAction);

        when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
        doReturn("1.0.0").when(spy).getInputName();
        doReturn(InputTagState.ABSENT).when(spy).getInputState();
        doReturn("789123").when(spy).getInputFrom();

        doNothing().when(spy).connectApi();

        when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
        doReturn(Optional.empty()).when(spy).getTagGHRef("1.0.0");
        doNothing().when(spy).deleteGHRef(Optional.empty());

        spy.execute();

        verify(this.ghActionsKitMock).getGitHubRepository();

        verify(spy).getInputName();
        verify(spy).getInputState();
        verify(spy).getInputFrom();
        verify(spy).connectApi();
        verify(spy).getTagGHRef("1.0.0");
        verify(spy).deleteGHRef(Optional.empty());

        verify(this.ghApiMock).getRepository("octocat/Hello-World");
        verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.REF.key());
        verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.NAME.key());
        verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.SHA.key());
    }

    /**
     * Test method.
     */
    @Test
    void whenConnectApi_thenVerifyOK() throws Exception {
        when(ghActionsKitMock.getRequiredEnv("GITHUB_TOKEN")).thenReturn("token");
        when(ghActionsKitMock.getGitHubApiUrl()).thenReturn("https://api.github.com");

        this.githubAction.connectApi();

        verify(ghActionsKitMock).getRequiredEnv("GITHUB_TOKEN");
        verify(ghActionsKitMock).getGitHubApiUrl();
        verify(ghActionsKitMock, times(2)).debug(Mockito.anyString());
        verify(ghApiMock).checkApiUrlValidity();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetTagGHRefExist_thenReturnRef() throws Exception {
        var ghRef1 = Mockito.mock(GHRef.class);
        when(ghRef1.getRef()).thenReturn("refs/tags/1.0.1");

        var ghRef2 = Mockito.mock(GHRef.class);
        when(ghRef2.getRef()).thenReturn("refs/tags/1.0.0");

        when(ghRepositoryMock.getRefs("tags")).thenReturn(new GHRef[] {ghRef1, ghRef2});

        assertThat(this.githubAction.getTagGHRef("1.0.0")).isPresent().contains(ghRef2);

        verify(ghRepositoryMock).getRefs("tags");
        verify(ghRef1).getRef();
        verify(ghRef2).getRef();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetTagGHRefDoesNotExist_thenReturnEmpty() throws Exception {
        when(ghRepositoryMock.getRefs("tags")).thenReturn(new GHRef[] {});

        assertThat(this.githubAction.getTagGHRef("1.0.0")).isEmpty();

        verify(ghRepositoryMock).getRefs("tags");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetTagGHRefRepoHasNoTags_thenReturnEmpty() throws Exception {
        when(ghRepositoryMock.getRefs("tags")).thenThrow(GHFileNotFoundException.class);

        assertThat(this.githubAction.getTagGHRef("1.0.0")).isEmpty();

        verify(ghRepositoryMock).getRefs("tags");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetTagGHRefNull_thenThrowNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.getTagGHRef(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenGetAnyGHRefExist_thenReturnRef() throws Exception {
        var ghRef1 = Mockito.mock(GHRef.class);
        when(ghRef1.getRef()).thenReturn("refs/heads/main");

        var ghRef2 = Mockito.mock(GHRef.class);
        when(ghRef2.getRef()).thenReturn("refs/heads/BRANCH-name");

        var ghRef3 = Mockito.mock(GHRef.class);
        when(ghRef3.getRef()).thenReturn("refs/tags/1.0.0");

        when(ghRepositoryMock.getRefs()).thenReturn(new GHRef[] {ghRef1, ghRef2, ghRef3});

        assertThat(this.githubAction.getAnyGHRef("refs/heads/main")).isPresent().contains(ghRef1);
        assertThat(this.githubAction.getAnyGHRef("branch-name")).isPresent().contains(ghRef2);
        assertThat(this.githubAction.getAnyGHRef("1.0.0")).isPresent().contains(ghRef3);
        assertThat(this.githubAction.getAnyGHRef("refs/tags/1.0.0")).isPresent().contains(ghRef3);

        verify(ghRepositoryMock, times(4)).getRefs();
        verify(ghRef1, times(4)).getRef();
        verify(ghRef2, times(3)).getRef();
        verify(ghRef3, times(2)).getRef();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetAnyGHRefDoesNotExist_thenReturnEmpty() throws Exception {
        when(ghRepositoryMock.getRefs()).thenReturn(new GHRef[] {});

        assertThat(this.githubAction.getAnyGHRef("branch-name")).isEmpty();

        verify(ghRepositoryMock).getRefs();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetAnyGHRefNull_thenThrowNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.getAnyGHRef(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenCreateGHRefPresentExistingTag_thenUpdateExistingTag() throws Exception {
        var ghRef = Mockito.mock(GHRef.class);

        this.githubAction.createGHRef("1.0.0", "123456", Optional.of(ghRef));

        verify(ghActionsKitMock).notice(Mockito.anyString());
        verify(ghRef).updateTo("123456", true);
    }

    /**
     * Test method.
     */
    @Test
    void whenCreateGHRefEmptyExistingTag_thenCreateTag() throws Exception {

        this.githubAction.createGHRef("refs/tags/1.0.0", "123456", Optional.empty());

        verify(ghActionsKitMock).notice(Mockito.anyString());
        verify(ghRepositoryMock).createRef("refs/tags/1.0.0", "123456");
    }

    /**
     * Test method.
     */
    @Test
    void whenCreateGHRefNull_thenThrowNullPointerException() throws Exception {
        var emptyOptional = Optional.<GHRef>empty();
        assertThrows(NullPointerException.class, () -> this.githubAction.createGHRef(null, "123456", emptyOptional));
        assertThrows(
                NullPointerException.class,
                () -> this.githubAction.createGHRef("refs/tags/1.0.0", null, emptyOptional));
        assertThrows(
                NullPointerException.class, () -> this.githubAction.createGHRef("refs/tags/1.0.0", "123456", null));
    }

    /**
     * Test method.
     */
    @Test
    void whenDeleteGHRefPresent_thenDeleteGhRef() throws Exception {
        var ghRef = Mockito.mock(GHRef.class);

        assertDoesNotThrow(() -> {
            this.githubAction.deleteGHRef(Optional.of(ghRef));
        });

        verify(ghActionsKitMock).notice(Mockito.anyString());
        verify(ghRef).delete();
    }

    /**
     * Test method.
     */
    @Test
    void whenDeleteGHRefEmpty_thenLogMessage() {
        assertDoesNotThrow(() -> {
            this.githubAction.deleteGHRef(Optional.empty());
        });

        verify(ghActionsKitMock).notice(Mockito.anyString());
    }

    /**
     * Test method.
     */
    @Test
    void whenDeleteGHRefNull_thenThrowNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.deleteGHRef(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenBranchRefNamePresent_thenReturnRefValue() {
        assertThat(this.githubAction.branchRef("branch-name")).isEqualTo("refs/heads/branch-name");
    }

    /**
     * Test method.
     */
    @Test
    void whenBranchRefNameNull_thenThrowNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.branchRef(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenTagRefNamePresent_thenReturnRefValue() {
        assertThat(this.githubAction.tagRef("1.0.0")).isEqualTo("refs/tags/1.0.0");
    }

    /**
     * Test method.
     */
    @Test
    void whenTagRefNameNull_thenThrowNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.tagRef(null));
    }
}
