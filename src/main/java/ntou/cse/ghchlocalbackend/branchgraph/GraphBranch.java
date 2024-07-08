package ntou.cse.ghchlocalbackend.branchgraph;

import lombok.*;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class GraphBranch {

    private String name;

    private Date endTime;

    private Date startTime;

    private String committer;

    public GraphBranch(String name, Date endTime, Date startTime, String committer) {
        this.name = name;
        this.endTime = endTime;
        this.startTime = startTime;
        this.committer = committer;
    }
}
