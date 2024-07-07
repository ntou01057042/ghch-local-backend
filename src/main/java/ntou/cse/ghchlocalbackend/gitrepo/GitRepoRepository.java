package ntou.cse.ghchlocalbackend.gitrepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitRepoRepository extends MongoRepository<GitRepo, String> {
    GitRepo findByRepoOwnerAndRepoName(String owner, String repo);
}
