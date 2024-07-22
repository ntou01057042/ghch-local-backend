package ntou.cse.ghchlocalbackend.branchgraph;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphBranchRepository extends MongoRepository<GraphBranch, String> {
    boolean existsByOwnerAndRepo(String owner, String repo);

    void deleteByOwnerAndRepo(String owner, String repo);

    List<GraphBranch> findAllByOwnerAndRepo(String owner, String repo);
}
