package ntou.cse.ghchlocalbackend.branchgraph;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

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

    public CloudGraphCommit(String owner, String repo, String branchName, String message, String committer) {
        this.owner = owner;
        this.repo = repo;
        this.branchName = branchName;
        this.message = message;
        this.committer = committer;
    }
}
