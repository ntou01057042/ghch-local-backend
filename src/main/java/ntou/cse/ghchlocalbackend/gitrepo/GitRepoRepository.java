package ntou.cse.ghchlocalbackend.gitrepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GitRepoRepository extends MongoRepository<GitRepo, String> {
    GitRepo findByRepoOwnerAndRepoName(String owner, String repo);

    List<GitRepo> findAllByRepoOwnerAndRepoName(String repoOwner, String repoName);
}
