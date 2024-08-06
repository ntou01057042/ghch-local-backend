package ntou.cse.ghchlocalbackend.branchgraph;

import ntou.cse.ghchlocalbackend.gitrepo.GitRepoRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/graph")
public class GraphController {

    private final GitRepoRepository gitRepoRepository;
    private final GraphBranchRepository graphBranchRepository;
    private final GraphCommitRepository graphCommitRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public GraphController(GitRepoRepository gitRepoRepository, GraphBranchRepository graphBranchRepository, GraphCommitRepository graphCommitRepository) {
        this.gitRepoRepository = gitRepoRepository;
        this.graphBranchRepository = graphBranchRepository;
        this.graphCommitRepository = graphCommitRepository;
    }

    @GetMapping
    public List<GraphBranch> getGraphBranchesAndGraphCommits(@RequestParam String owner, @RequestParam String repo) throws IOException, GitAPIException {
        // delete old records
        if (graphBranchRepository.existsByOwnerAndRepo(owner, repo)) {
            graphBranchRepository.deleteByOwnerAndRepo(owner, repo);
        }
        if (graphCommitRepository.existsByOwnerAndRepo(owner, repo)) {
            graphCommitRepository.deleteByOwnerAndRepo(owner, repo);
        }

        List<GraphBranch> res = new ArrayList<>();
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
//        File repoDir = new File(gitRepoRepository.findByRepoOwnerAndRepoName(owner, repo).getDirectory());
        File repoDir = openTargetRepository(owner, repo);
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            System.out.println("Having repository: " + repository.getDirectory());

            // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
            Ref masterhead = repository.exactRef("refs/remotes/origin/master");
            if (masterhead == null) {
                masterhead = repository.exactRef("refs/remotes/origin/main");
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

            for (String ref : listBranches(owner, repo)) {
                if (ref.startsWith("refs/heads/") || ref.startsWith("refs/remotes/origin/master") || ref.startsWith("refs/remotes/origin/main")) {
                    continue;
                }
//                System.out.println("ref: " + ref);
                // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
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
                        ref.substring("refs/remotes/origin/".length()),
                        new Date(lastCommit.getCommitTime() * 1000L),
                        new Date(firstCommit.getCommitTime() * 1000L),
                        lastCommit.getCommitterIdent().getName()
                );
                System.out.println(graphBranch);
                res.add(graphBranch);
                graphBranchRepository.save(graphBranch);

                // Store GraphCommits in this branch
                List<GraphCommit> graphCommits = new ArrayList<>();
                for (int i = commits.indexOf(firstCommit); i >= 0; i--) {
                    graphCommits.add(new GraphCommit(
                            owner,
                            repo,
                            ref.substring("refs/remotes/origin/".length()),
                            commits.get(i).getShortMessage(),
                            commits.get(i).getCommitterIdent().getName(),
                            new Date(commits.get(i).getCommitTime() * 1000L)
                    ));
                }
                System.out.println(graphCommits);
                graphCommitRepository.saveAll(graphCommits);
            }
        }
        return res;
    }

    private RevCommit findFirstCommit(List<RevCommit> commits, List<RevCommit> masterCommits) {
        RevCommit firstCommit = null;
        Set<RevCommit> branchCommits = new HashSet<>(commits);
        for (RevCommit commit : masterCommits) {
            if (branchCommits.contains(commit)) {
                // This commit is where the branch was created
                firstCommit = commits.get(commits.indexOf(commit) - 1);
                break;
            }
        }
        return firstCommit;
    }

    private File openTargetRepository(String owner, String repo) {
        return new File(gitRepoRepository.findByRepoOwnerAndRepoName(owner, repo).getDirectory());
    }

    public List<String> listBranches(String owner, String repo) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);
        List<String> refs = new ArrayList<>();
        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            try (Git git = new Git(repository)) {
//                System.out.println("Listing local branches:");
                List<Ref> call = git.branchList().call();
//                for (Ref ref : call) {
//                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                    System.out.println("Branch: " + ref.getName());
//                }

//                System.out.println("Now including remote branches:");
                call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
                for (Ref ref : call) {
//                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
//                    System.out.println("Branch: " + ref.getName());
                    refs.add(ref.getName());
                }
            }
        }
        return refs;
    }

    @GetMapping("/commits")
    public ResponseEntity<List<GraphCommit>> getGraphCommits(@RequestParam String owner, @RequestParam String repo) {
        List<GraphCommit> result = graphCommitRepository.findAllByOwnerAndRepo(owner, repo);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload")
    public void uploadGraphBranchesAndGraphCommits(@RequestParam String owner, @RequestParam String repo) {
        restTemplate.delete("http://localhost:8081/cloud-graph-branch/" + owner + "/" + repo);

        List<GraphBranch> graphBranches = graphBranchRepository.findAllByOwnerAndRepo(owner, repo);
        for (GraphBranch graphBranch : graphBranches) {
            CloudGraphBranch newCloudGraphBranchRequest = new CloudGraphBranch(
                    graphBranch.getOwner(),
                    graphBranch.getRepo(),
                    graphBranch.getName(),
                    graphBranch.getEndTime(),
                    graphBranch.getStartTime(),
                    graphBranch.getCommitter()
            );
            restTemplate.postForEntity(
                    "http://localhost:8081/cloud-graph-branch",
                    newCloudGraphBranchRequest,
                    Void.class
            );
        }

        restTemplate.delete("http://localhost:8081/cloud-graph-commit/" + owner + "/" + repo);

        List<GraphCommit> graphCommits = graphCommitRepository.findAllByOwnerAndRepo(owner, repo);
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
                    "http://localhost:8081/cloud-graph-commit",
                    newCloudGraphCommitRequest,
                    Void.class
            );
        }
    }
}
