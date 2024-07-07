package ntou.cse.ghchlocalbackend.gitrepo;

import ntou.cse.ghchlocalbackend.AppUserService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.URI;

@RestController
@RequestMapping("/git-repos")
public class GitRepoController {

    private final GitRepoRepository gitRepoRepository;

    private final AppUserService appUserService;

    public GitRepoController(GitRepoRepository gitRepoRepository, AppUserService appUserService) {
        this.gitRepoRepository = gitRepoRepository;
        this.appUserService = appUserService;
    }

    @PostMapping("/clone")
    public ResponseEntity<Void> cloneGitHubRepo(@RequestParam String repoOwner, @RequestParam String repoName, UriComponentsBuilder ucb) throws GitAPIException {
        // prepare a new folder for the cloned repository
        String desktopPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "GHCH";   // temporary
        String url = "https://github.com/" + repoOwner + "/" + repoName + ".git";
        File directory = new File(desktopPath, repoName);

        // then clone
        System.out.println("Cloning from " + url + " to " + directory);
        try (Git result = Git.cloneRepository()
                .setURI(url)
                .setDirectory(directory)
                .setProgressMonitor(new TextProgressMonitor())
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            System.out.println("Having repository: " + result.getRepository().getDirectory());
        }

        GitRepo savedGitRepo = gitRepoRepository.save(new GitRepo(appUserService.getAppUser().getUsername(), repoOwner, repoName));
        URI locationOfNewGitRepo = ucb
                .path("/git-repos/{id}")
                .buildAndExpand(savedGitRepo.getId())
                .toUri();
        return ResponseEntity.created(locationOfNewGitRepo).build();
    }
}
