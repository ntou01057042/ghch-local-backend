package ntou.cse.ghchlocalbackend.flowcommit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class FlowCommit {

    private String owner;

    private String repo;

    private String flowBranch;   // main or unmerged branch

    private String message;

    private String committer;

    private Date commitTime;

    private Boolean isMainBranchCommit;

    private Boolean isMergeCommit;

    public FlowCommit(String owner, String repo, String flowBranch, String message, String committer, Date commitTime, Boolean isMainBranchCommit, Boolean isMergeCommit) {
        this.owner = owner;
        this.repo = repo;
        this.flowBranch = flowBranch;
        this.message = message;
        this.committer = committer;
        this.commitTime = commitTime;
        this.isMainBranchCommit = isMainBranchCommit;
        this.isMergeCommit = isMergeCommit;
    }
}
