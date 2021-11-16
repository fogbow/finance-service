package cloud.fogbow.fs.core.datastore;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;

@Repository
@Transactional
public interface FinancePlanRepository extends JpaRepository<PersistablePlanPlugin, String>{
}
