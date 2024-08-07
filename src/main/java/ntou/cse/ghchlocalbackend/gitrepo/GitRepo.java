package ntou.cse.ghchlocalbackend.gitrepo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("git-repos")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class GitRepo {

    @Id
    private String id;

    private String repoOwner;

    private String repoName;

    private String directory;

    private Date clonedDate;

    public GitRepo(String repoOwner, String repoName, String directory, Date clonedDate) {
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.directory = directory;
        this.clonedDate = clonedDate;
    }
}
