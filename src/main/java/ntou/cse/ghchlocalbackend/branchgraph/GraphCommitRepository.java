package ntou.cse.ghchlocalbackend.branchgraph;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphCommitRepository extends MongoRepository<GraphCommit, String> {
    boolean existsByOwnerAndRepo(String owner, String repo);

    void deleteByOwnerAndRepo(String owner, String repo);

    List<GraphCommit> findAllByOwnerAndRepo(String owner, String repo);
}
