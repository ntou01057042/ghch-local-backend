package ntou.cse.ghchlocalbackend.branchgraph;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("graph-branches")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class GraphBranch {

    private String owner;

    private String repo;

    private String name;

    private Date endTime;

    private Date startTime;

    private String committer;

    public GraphBranch(String owner, String repo, String name, Date endTime, Date startTime, String committer) {
        this.owner = owner;
        this.repo = repo;
        this.name = name;
        this.endTime = endTime;
        this.startTime = startTime;
        this.committer = committer;
    }
}
