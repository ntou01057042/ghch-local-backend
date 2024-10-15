package ntou.cse.ghchlocalbackend.branch;

import ntou.cse.ghchlocalbackend.LoginController;
import ntou.cse.ghchlocalbackend.branchgraph.*;
import ntou.cse.ghchlocalbackend.gitrepo.GitRepoRepository;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/branch")
public class BranchController {

    private final GitRepoRepository gitRepoRepository;
    private final GraphBranchRepository graphBranchRepository;
    private final GraphCommitRepository graphCommitRepository;

    private final LoginController loginController;

    private final RestTemplate restTemplate = new RestTemplate();

    public BranchController(GitRepoRepository gitRepoRepository, GraphBranchRepository graphBranchRepository, GraphCommitRepository graphCommitRepository, LoginController loginController) {
        this.gitRepoRepository = gitRepoRepository;
        this.graphBranchRepository = graphBranchRepository;
        this.graphCommitRepository = graphCommitRepository;
        this.loginController = loginController;
    }

    @GetMapping("/{owner}/{repo}")
    ResponseEntity<String> getCurrentLocalBranch(@PathVariable String owner, @PathVariable String repo) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            return ResponseEntity.ok(repository.getBranch());
        } catch (IOException e) {
            System.out.println("Catch IOException: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/push/{owner}/{repo}")
    ResponseEntity<Void> pushToGitHub(@PathVariable String owner, @PathVariable String repo) {
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
                    git.push()
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(loginController.getGitHubToken(), ""))
                            .setRemote("origin").add(repository.getBranch())
                            .call();
                } catch (GitAPIException e) {
                    System.out.println("Catch GitAPIException: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Catch IOException: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload/{owner}/{repo}")
    ResponseEntity<Void> uploadCurrentBranchToCloudServer(@PathVariable String owner, @PathVariable String repo, @RequestParam String branch) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            System.out.println(repository.getBranch());
            try (Git git = new Git(repository)) {
                try {
                    // Checkout and update main branch
                    String currentBranch = repository.getBranch();
//                    git.checkout().setName("master").call();
                    git.checkout().setName("main").call();
                    System.out.println(loginController.getGitHubToken());
                    git.pull()
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(loginController.getGitHubToken(), ""))
                            .setRemote("origin").setRemoteBranchName(repository.getBranch())
                            .call();

                    //
                    git.checkout().setName(currentBranch).call();

                    //
                    if (graphBranchRepository.existsByOwnerAndRepoAndName(owner, repo, branch)) {
                        graphBranchRepository.deleteByOwnerAndRepoAndName(owner, repo, branch);
                    }
                    if (graphCommitRepository.existsByOwnerAndRepoAndBranchName(owner, repo, branch)) {
                        graphCommitRepository.deleteByOwnerAndRepoAndBranchName(owner, repo, branch);
                    }

                    // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
                    Ref masterhead = repository.exactRef("refs/heads/master");
                    if (masterhead == null) {
                        masterhead = repository.exactRef("refs/heads/main");
                    }
                    System.out.println("\nRef: " + masterhead);

                    Set<RevCommit> masterSeen = new HashSet<>();
                    List<RevCommit> masterCommits = new ArrayList<>();

                    // a RevWalk allows to walk over commits based on some filtering that is defined
                    try (RevWalk walk = new RevWalk(repository)) {
                        RevCommit commit = walk.parseCommit(masterhead.getObjectId());
                        System.out.println("Start-Commit (Last commit): " + commit);

//                    System.out.println("Walking all commits starting at HEAD");
                        walk.markStart(commit);
                        masterSeen.add(commit);
//                    commits.add(commit);
                        int count = 0;
                        for (RevCommit rev : walk) {
                            if (!masterSeen.contains(rev)) {
                                continue;
                            }
                            masterCommits.add(rev);
//                    System.out.println("Master commit: " + rev);
                            RevCommit[] parents = rev.getParents();
                            if (parents.length > 0) {
//                        System.out.println("    First parent: " + parents[0]);
                                masterSeen.add(parents[0]);
                            }
//                        for (RevCommit parentRev : rev.getParents()) {
//                            System.out.println("    Parent: " + parentRev);
//                        }
                            count++;
                        }
                        System.out.println("Branch commits:");
                        for (RevCommit rev : masterCommits) {
                            System.out.println("    " + rev);
                        }
                        System.out.println(count);

                        walk.dispose();
                    }

                    // Update main branch
                    String mainBranchName = masterhead.getName().contains("master") ? "master" : "main";
                    if (graphBranchRepository.existsByOwnerAndRepoAndName(owner, repo, mainBranchName)) {
                        graphBranchRepository.deleteByOwnerAndRepoAndName(owner, repo, mainBranchName);
                    }
                    GraphBranch mainBranch = new GraphBranch(
                            owner,
                            repo,
                            masterhead.getName().contains("master") ? "master" : "main",
                            new Date(masterCommits.get(0).getCommitTime() * 1000L),
                            new Date(masterCommits.get(masterCommits.size() - 1).getCommitTime() * 1000L),
                            ""
                    );
                    graphBranchRepository.save(mainBranch);

                    // Update GraphBranch and GraphCommits
                    String ref = "refs/heads/" + currentBranch;
                    Ref head = repository.exactRef(ref);
                    System.out.println("\nRef: " + head);

                    RevCommit lastCommit;
                    Set<RevCommit> seen = new HashSet<>();
                    List<RevCommit> commits = new ArrayList<>();

                    // a RevWalk allows to walk over commits based on some filtering that is defined
                    try (RevWalk walk = new RevWalk(repository)) {
                        lastCommit = walk.parseCommit(head.getObjectId());
                        System.out.println("Start-Commit (Last commit): " + lastCommit);

//                    System.out.println("Walking all commits starting at HEAD");
                        walk.markStart(lastCommit);
                        seen.add(lastCommit);
//                    commits.add(commit);
                        int count = 0;
                        for (RevCommit rev : walk) {
                            if (!seen.contains(rev)) {
                                continue;
                            }
                            commits.add(rev);
//                        System.out.println("Commit: " + rev);
                            RevCommit[] parents = rev.getParents();
                            if (parents.length > 0) {
//                            System.out.println("    First parent: " + parents[0]);
                                seen.add(parents[0]);
                            }
//                        for (RevCommit parentRev : rev.getParents()) {
//                            System.out.println("    Parent: " + parentRev);
//                        }
                            count++;
                        }
                        System.out.println(count + " commits:");
                        for (RevCommit rev : commits) {
                            System.out.println("    " + rev);
                        }
//                    System.out.println(count);

                        walk.dispose();
                    }

                    // Find the first commit after this branch was created
                    RevCommit firstCommit = findFirstCommit(commits, masterCommits);
                    System.out.println("First commit: " + firstCommit);
                    GraphBranch graphBranch = new GraphBranch(
                            owner,
                            repo,
                            ref.substring("refs/heads/".length()),
                            new Date(lastCommit.getCommitTime() * 1000L),
                            new Date(firstCommit.getCommitTime() * 1000L),
                            firstCommit.getCommitterIdent().getName()
                    );
                    System.out.println(graphBranch);
                    graphBranchRepository.save(graphBranch);

                    // Upload GraphBranch for current branch
                    restTemplate.delete("https://ghch-cloud-server-b889208febef.herokuapp.com/cloud-graph-branch/" + owner + "/" + repo + "?branch=" + currentBranch);
                    CloudGraphBranch newCloudGraphBranchRequest = new CloudGraphBranch(
                            graphBranch.getOwner(),
                            graphBranch.getRepo(),
                            graphBranch.getName(),
                            graphBranch.getEndTime(),
                            graphBranch.getStartTime(),
                            graphBranch.getCommitter()
                    );
                    restTemplate.postForEntity(
                            "https://ghch-cloud-server-b889208febef.herokuapp.com/cloud-graph-branch",
                            newCloudGraphBranchRequest,
                            Void.class
                    );

                    // Store GraphCommits in this branch
                    List<GraphCommit> graphCommits = new ArrayList<>();
                    for (int i = commits.indexOf(firstCommit); i >= 0; i--) {
                        graphCommits.add(new GraphCommit(
                                owner,
                                repo,
                                ref.substring("refs/heads/".length()),
                                commits.get(i).getShortMessage(),
                                commits.get(i).getCommitterIdent().getName(),
                                new Date(commits.get(i).getCommitTime() * 1000L)
                        ));
                    }
                    System.out.println(graphCommits);
                    graphCommitRepository.saveAll(graphCommits);

                    // Upload GraphCommits of current branch
                    restTemplate.delete("https://ghch-cloud-server-b889208febef.herokuapp.com/cloud-graph-commit/" + owner + "/" + repo + "?branch=" + currentBranch);
                    for (GraphCommit graphCommit : graphCommits) {
                        CloudGraphCommit newCloudGraphCommitRequest = new CloudGraphCommit(
                                graphCommit.getOwner(),
                                graphCommit.getRepo(),
                                graphCommit.getBranchName(),
                                graphCommit.getMessage(),
                                graphCommit.getCommitter(),
                                graphCommit.getCommitTime()
                        );
                        restTemplate.postForEntity(
                                "https://ghch-cloud-server-b889208febef.herokuapp.com/cloud-graph-commit",
                                newCloudGraphCommitRequest,
                                Void.class
                        );
                    }
                } catch (GitAPIException e) {
                    System.out.println("Catch GitAPIException: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Catch IOException: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    private RevCommit findFirstCommit(List<RevCommit> commits, List<RevCommit> masterCommits) {
        RevCommit firstCommit = null;
        Set<RevCommit> branchCommits = new HashSet<>(commits);
        for (RevCommit commit : masterCommits) {
            if (branchCommits.contains(commit)) {
                // This commit is where the branch was created
                firstCommit = commits.indexOf(commit) > 0 ? commits.get(commits.indexOf(commit) - 1) : commits.get(0);
                break;
            }
        }
        return firstCommit;
    }

    private File openTargetRepository(String owner, String repo) {
        return new File(gitRepoRepository.findByRepoOwnerAndRepoName(owner, repo).getDirectory());
    }

    @PostMapping("/pull/{owner}/{repo}")
    ResponseEntity<Void> pullAllBranchesFromGitHub(@PathVariable String owner, @PathVariable String repo) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            System.out.println(repository.getBranch());
            try (Git git = new Git(repository)) {
                String currentBranch = repository.getBranch();
                // Checkout and "pull" each branch
                for (String ref : listLocalBranches(owner, repo)) {
//                    System.out.println(ref.substring("refs/heads/".length()));
                    git.checkout().setName(ref.substring("refs/heads/".length())).call();
                    git.pull()
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(loginController.getGitHubToken(), ""))
                            .setRemote("origin").setRemoteBranchName(repository.getBranch())
                            .call();
                }
                for (String branchName : getNewBranchesCreatedOnGitHub(owner, repo)) {
//                    System.out.println(branchName);
                    git.checkout()
                            .setCreateBranch(true)
                            .setName(branchName)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                            .setStartPoint("refs/remotes/origin/" + branchName)
                            .call();
                }
                git.checkout().setName(currentBranch).call();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.out.println("Catch IOException: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    public List<String> getNewBranchesCreatedOnGitHub(String owner, String repo) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        List<String> res = new ArrayList<>();
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            try (Git git = new Git(repository)) {
//                System.out.println("Listing local branches:");
//                List<Ref> call = git.branchList().call();
//                for (Ref ref : call) {
//                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                    System.out.println("Branch: " + ref.getName());
//                }

//                System.out.println("Now including remote branches:");
                List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
                Map<String, Boolean> map = new HashMap<>();
                for (Ref ref : call) {
//                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                    System.out.println("Branch: " + ref.getName());
//                    if (ref.getName().startsWith("refs/heads/")) {
//                        refs.add(ref.getName());
//                    }
                    if (ref.getName().startsWith("refs/heads/")) {
                        map.put(ref.getName().substring("refs/heads/".length()), true);
                    } else if (ref.getName().startsWith("refs/remotes/origin")) {
                        String branchName = ref.getName().substring("refs/remotes/origin/".length());
                        if (!map.containsKey(branchName) && !branchName.equals("master")) {
                            res.add(ref.getName().substring("refs/remotes/origin/".length()));
                        }
                    }
                }
            }
        }
        return res;
    }

    public List<String> listLocalBranches(String owner, String repo) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        List<String> refs = new ArrayList<>();
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            try (Git git = new Git(repository)) {
//                System.out.println("Listing local branches:");
//                List<Ref> call = git.branchList().call();
//                for (Ref ref : call) {
//                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                    System.out.println("Branch: " + ref.getName());
//                }

//                System.out.println("Now including remote branches:");
                List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
                for (Ref ref : call) {
//                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                    System.out.println("Branch: " + ref.getName());
                    if (ref.getName().startsWith("refs/heads/")) {
                        refs.add(ref.getName());
                    }
                }
            }
        }
        return refs;
    }

    @GetMapping("/latest-or-not")
    ResponseEntity<String> checkLocalBranchStatus(@RequestParam String owner, @RequestParam String repo, @RequestParam String localBranch) {
        File repoDir = openTargetRepository(owner, repo);
        try {
            // Open the existing repository
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repoDir)
//                    .setGitDir(new File("C:\\Users\\baekhil\\Documents\\GitHub\\create-pr-using-github-rest-api\\.git"))
                    .build();

            try (Git git = new Git(repository)) {
                // Fetch from the remote
                git.fetch()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(loginController.getGitHubToken(), ""))
                        .setRemote("origin")
                        .call();

                // Get the commit IDs for both branches
                ObjectId localHead = repository.resolve(localBranch);
                ObjectId remoteHead = repository.resolve("origin/" + localBranch);

                // Compare the commits
                if (localHead.equals(remoteHead)) {
                    return ResponseEntity.ok("Your branch is up to date with the remote.");
                } else {
                    return ResponseEntity.ok("Your branch is not up to date with the remote.");
                }
            }
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/create/{owner}/{repo}")
    ResponseEntity<Void> createANewBranch(@PathVariable String owner, @PathVariable String repo, @RequestParam String newBranchName) {
        File repoDir = openTargetRepository(owner, repo);
        try {
            // Open the existing repository
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repoDir)
                    .build();

            try (Git git = new Git(repository)) {
                // run the add-call
                git.branchCreate()
                        .setName(newBranchName)
                        .call();
                git.checkout().setName(newBranchName).call();
//                for (Ref ref : git.branchList().call()) {
//                    System.out.println("Branch-Created: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                }
            }
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync-main/{owner}/{repo}")
    ResponseEntity<Void> syncWithMainBranch(@PathVariable String owner, @PathVariable String repo) {
        File repoDir = openTargetRepository(owner, repo);
        try {
            // Open the existing repository
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repoDir)
                    .build();

            try (Git git = new Git(repository)) {
                String currentBranch = repository.getBranch();
                System.out.println(currentBranch);
                // retrieve the objectId of the latest commit on main branch
                ObjectId mergeBase = repository.resolve("main");

                // perform the actual merge, here we disable FastForward to see the
                // actual merge-commit even though the merge is trivial
                MergeResult merge = git.merge().
                        include(mergeBase).
                        setCommit(true).
                        setFastForward(MergeCommand.FastForwardMode.NO_FF).
                        setMessage("Merge changes from main branch").
                        call();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }
}
