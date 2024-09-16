package ntou.cse.ghchlocalbackend.branch;

import ntou.cse.ghchlocalbackend.LoginController;
import ntou.cse.ghchlocalbackend.gitrepo.GitRepoRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/push")
public class PushController {

    private final GitRepoRepository gitRepoRepository;

    private final LoginController loginController;

    public PushController(GitRepoRepository gitRepoRepository, LoginController loginController) {
        this.gitRepoRepository = gitRepoRepository;
        this.loginController = loginController;
    }

    @PostMapping("/{owner}/{repo}/{branch}")
    ResponseEntity<Void> pushToGitHubAndUploadToCloudServer(@PathVariable String owner, @PathVariable String repo, @PathVariable String branch) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            System.out.println(repository.getBranch());
            try (Git git = new Git(repository)) {
                try {
                    System.out.println(loginController.getGitHubToken());
                    git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(loginController.getGitHubToken(), "")).setRemote("origin").add(repository.getBranch()).call();
                } catch (GitAPIException e) {
                    System.out.println("Catch GitAPIException: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Catch IOException: " + e.getMessage());
        }
        return null;
    }

    private File openTargetRepository(String owner, String repo) {
        return new File(gitRepoRepository.findByRepoOwnerAndRepoName(owner, repo).getDirectory());
    }
}
