package ntou.cse.ghchlocalbackend.flowcommit;

import ntou.cse.ghchlocalbackend.gitrepo.GitRepoRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/flow-commit")
public class FlowCommitController {

    private final GitRepoRepository gitRepoRepository;

    public FlowCommitController(GitRepoRepository gitRepoRepository) {
        this.gitRepoRepository = gitRepoRepository;
    }

    @GetMapping("/{owner}/{repo}")
    public List<FlowCommit> getFlowCommits(@PathVariable String owner, @PathVariable String repo, @RequestParam String branch) throws IOException, GitAPIException {
        Map<String, FlowCommit> res = new HashMap<>();
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = openTargetRepository(owner, repo);

//        for (String branchName : listBranches(owner, repo)) {
//            System.out.println(branchName);
//        }

        // refs/heads/{branch}
        // refs/remotes/origin/{branch}

        try (Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {

//            System.out.println("Having repository: " + repository.getDirectory());

            // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
            Ref mainBranch = repository.exactRef("refs/heads/master");
            if (mainBranch == null) {
                mainBranch = repository.exactRef("refs/heads/main");
            }
//            if (masterhead == null) {
//                masterhead = repository.exactRef("refs/remotes/origin/main");
//            }
//            if (masterhead == null) {
//                masterhead = repository.exactRef("refs/remotes/origin/main");
//            }
//            System.out.println("\nmainBranch: " + mainBranch);

            Set<RevCommit> mainBranchSeen = new HashSet<>();
            List<RevCommit> mainBranchCommits = new ArrayList<>();

            // Get all base commits in the main branch
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(mainBranch.getObjectId());
//                System.out.println("Start-Commit (Last commit): " + commit);

//                    System.out.println("Walking all commits starting at HEAD");
                walk.markStart(commit);
                mainBranchSeen.add(commit);
//                    commits.add(commit);
                int count = 0;
                for (RevCommit rev : walk) {
//                    System.out.println(rev);
                    if (!mainBranchSeen.contains(rev)) {
                        continue;
                    }
                    mainBranchCommits.add(rev);
//                    System.out.println("Master commit: " + rev);
                    RevCommit[] parents = rev.getParents();
                    if (parents.length > 0) {
//                        System.out.println("    First parent: " + parents[0]);
                        mainBranchSeen.add(parents[0]);
                    }
//                        for (RevCommit parentRev : rev.getParents()) {
//                            System.out.println("    Parent: " + parentRev);
//                        }
                    count++;
                }
//                System.out.println("Main branch commits:");
//                for (RevCommit rev : mainBranchCommits) {
//                    System.out.println("    " + rev);
//                }
//                System.out.println(count);

                walk.dispose();
            }

            String branchRef = "refs/heads/" + branch;
//            System.out.println(branchRef);

            Ref head = repository.exactRef(branchRef);
//            System.out.println("\nRef: " + head);

            RevCommit lastCommit;
            Set<RevCommit> seen = new HashSet<>();
            List<RevCommit> commits = new ArrayList<>();

            // Get all commits in the target branch
            try (RevWalk walk = new RevWalk(repository)) {
                lastCommit = walk.parseCommit(head.getObjectId());
//                System.out.println("Start-Commit (Last commit): " + lastCommit);
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
//                System.out.println(count + " commits:");
//                for (RevCommit rev : commits) {
//                    System.out.println("    " + rev);
//                }
//                    System.out.println(count);
                walk.dispose();
            }

            // 1. Iterate all commits in the main branch to find if the merge commit exists
            for (RevCommit rev : mainBranchCommits) {
//                System.out.println();
//                System.out.println(Arrays.toString(rev.getParents()));
                if (rev.getParentCount() > 0) {
                    for (RevCommit parent : rev.getParents()) {
//                        System.out.println(parent.getFullMessage());
                        // If the new branch is merged into main branch
                        if (parent.getFullMessage().equals(commits.get(0).getFullMessage())) {
                            System.out.println("Found merge point in the main branch!");
                            res.put(rev.getFullMessage(), new FlowCommit(
                                    owner,
                                    repo,
                                    branch,
                                    rev.getFullMessage(),
                                    rev.getCommitterIdent().getName(),
                                    new Date(rev.getCommitTime() * 1000L),
                                    true,
                                    true
                            ));
                        }
                    }
                }
            }

            // 2. Iterate all commits in new branch
            Set<RevCommit> mainBranchCommitSet = new HashSet<>(mainBranchCommits);
            for (RevCommit rev : commits) {
//                System.out.println();
//                System.out.println(Arrays.toString(rev.getParents()));
                // If it is a merge commit in the new branch
                if (rev.getParentCount() == 2) {
                    System.out.println("Found merge point in the new branch!");
                    // Add this merge point
                    res.put(rev.getFullMessage(), new FlowCommit(
                            owner,
                            repo,
                            branch,
                            rev.getFullMessage(),
                            rev.getCommitterIdent().getName(),
                            new Date(rev.getCommitTime() * 1000L),
                            false,
                            true
                    ));
                    // The second parent is in main branch
                    res.put(rev.getFullMessage(), new FlowCommit(
                            owner,
                            repo,
                            branch,
                            rev.getParent(1).getFullMessage(),
                            rev.getParent(1).getCommitterIdent().getName(),
                            new Date(rev.getParent(1).getCommitTime() * 1000L),
                            true,
                            false
                    ));
                } else if (rev.getParentCount() == 1) {
                    // Add normal commit in the new branch
                    res.put(rev.getFullMessage(), new FlowCommit(
                            owner,
                            repo,
                            branch,
                            rev.getFullMessage(),
                            rev.getCommitterIdent().getName(),
                            new Date(rev.getCommitTime() * 1000L),
                            false,
                            false
                    ));
                    // Found the first commit in the main branch
                    if (mainBranchCommitSet.contains(rev.getParent(0))) {
                        res.put(rev.getFullMessage(), new FlowCommit(
                                owner,
                                repo,
                                branch,
                                rev.getParent(0).getFullMessage(),
                                rev.getParent(0).getCommitterIdent().getName(),
                                new Date(rev.getParent(0).getCommitTime() * 1000L),
                                true,
                                false
                        ));
                        break;
                    }
                }
            }

            // 3. Iterate all commits in the main branch to find if any unmerged commit exists
            for (RevCommit rev : mainBranchCommits) {
//                System.out.println();
//                System.out.println(Arrays.toString(rev.getParents()));
                if (rev.getParentCount() == 1) {
                    for (RevCommit parent : rev.getParents()) {
//                        System.out.println(parent.getFullMessage());
                        if (!res.containsKey(parent.getFullMessage())) {
                            System.out.println("Found unmerge commit in the main branch!");
                            res.put(rev.getFullMessage(), new FlowCommit(
                                    owner,
                                    repo,
                                    branch,
                                    rev.getFullMessage(),
                                    rev.getCommitterIdent().getName(),
                                    new Date(rev.getCommitTime() * 1000L),
                                    true,
                                    false
                            ));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(res.values());
    }

    private File openTargetRepository(String owner, String repo) {
        return new File(gitRepoRepository.findByRepoOwnerAndRepoName(owner, repo).getDirectory());
    }

//    public List<String> listBranches(String owner, String repo) throws IOException, GitAPIException {
//        FileRepositoryBuilder builder = new FileRepositoryBuilder();
//        File repoDir = openTargetRepository(owner, repo);
//        List<String> refs = new ArrayList<>();
//        try (Repository repository = builder.setGitDir(repoDir)
//                .readEnvironment() // scan environment GIT_* variables
//                .findGitDir() // scan up the file system tree
//                .build()) {
//            try (Git git = new Git(repository)) {
////                System.out.println("Listing local branches:");
//                List<Ref> call = git.branchList().call();
////                for (Ref ref : call) {
////                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
////                    System.out.println("Branch: " + ref.getName());
////                }
//
////                System.out.println("Now including remote branches:");
//                call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
//                for (Ref ref : call) {
////                    System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
////                    System.out.println("Branch: " + ref.getName());
//                    refs.add(ref.getName());
//                }
//            }
//        }
//        return refs;
//    }
}
