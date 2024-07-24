package ntou.cse.ghchlocalbackend.branchgraph;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class CloudGraphCommit {

    @Id
    private String id;

    private String owner;

    private String repo;

    private String branchName;

    private String message;

    private String committer;

    private Date commitTime;

    public CloudGraphCommit(String owner, String repo, String branchName, String message, String committer, Date commitTime) {
        this.owner = owner;
        this.repo = repo;
        this.branchName = branchName;
        this.message = message;
        this.committer = committer;
        this.commitTime = commitTime;
    }
}
