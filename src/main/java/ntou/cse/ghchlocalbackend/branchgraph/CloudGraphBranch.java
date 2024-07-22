package ntou.cse.ghchlocalbackend.branchgraph;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class CloudGraphBranch {

    @Id
    private String id;

    private String owner;

    private String repo;

    private String name;

    private Date endTime;

    private Date startTime;

    private String committer;

    public CloudGraphBranch(String owner, String repo, String name, Date endTime, Date startTime, String committer) {
        this.owner = owner;
        this.repo = repo;
        this.name = name;
        this.endTime = endTime;
        this.startTime = startTime;
        this.committer = committer;
    }
}
