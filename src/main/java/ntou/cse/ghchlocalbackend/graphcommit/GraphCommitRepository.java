package ntou.cse.ghchlocalbackend.graphcommit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraphCommitRepository extends MongoRepository<GraphCommit, String> {
    boolean existsByOwnerAndRepo(String owner, String repo);

    void deleteByOwnerAndRepo(String owner, String repo);
}
