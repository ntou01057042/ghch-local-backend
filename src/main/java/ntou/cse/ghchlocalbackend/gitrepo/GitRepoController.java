package ntou.cse.ghchlocalbackend.gitrepo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.URI;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/git-repo")
public class GitRepoController {

    private final GitRepoRepository gitRepoRepository;

    public GitRepoController(GitRepoRepository gitRepoRepository) {
        this.gitRepoRepository = gitRepoRepository;
    }

    @PostMapping("/clone")
    public ResponseEntity<Void> cloneGitHubRepo(@RequestParam String repoOwner, @RequestParam String repoName, UriComponentsBuilder ucb) throws GitAPIException {
        // prepare a new folder for the cloned repository
        String desktopPath = System.getProperty("user.home") + File.separator + "GHCH";   // temporary
        String url = "https://github.com/" + repoOwner + "/" + repoName + ".git";
        File directory = new File(desktopPath, repoName);
        String resultDirectory;

        // then clone
        System.out.println("Cloning from " + url + " to " + directory);
        try (Git result = Git.cloneRepository()
                .setURI(url)
                .setDirectory(directory)
                .setProgressMonitor(new TextProgressMonitor())
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            System.out.println("Having repository: " + result.getRepository().getDirectory());
            resultDirectory = result.getRepository().getDirectory().toString();
        }

//        GitRepo savedGitRepo = gitRepoRepository.save(new GitRepo(appUserService.getAppUser().getUsername(), repoOwner, repoName, resultDirectory));
        GitRepo savedGitRepo = gitRepoRepository.save(new GitRepo(repoOwner, repoName, resultDirectory));
        URI locationOfNewGitRepo = ucb
                .path("/git-repos/{id}")
                .buildAndExpand(savedGitRepo.getId())
                .toUri();
        return ResponseEntity.created(locationOfNewGitRepo).build();
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkIfClonedRepoExists(@RequestParam String repoOwner, @RequestParam String repoName) {
        GitRepo gitRepo = gitRepoRepository.findByRepoOwnerAndRepoName(repoOwner, repoName);
        if (gitRepo != null) {
            System.out.println(gitRepo.getDirectory());
            File file = new File(gitRepo.getDirectory());
            if (file.exists()) {
                System.out.println("The Git repository exists.");
                return ResponseEntity.ok(true);
            } else {
                System.out.println("The Git repository does not exist.");
            }
        }
        return ResponseEntity.ok(false);
    }
}
