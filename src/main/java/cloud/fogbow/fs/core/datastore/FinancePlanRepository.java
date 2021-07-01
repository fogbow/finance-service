package cloud.fogbow.fs.core.datastore;

import org.springframework.data.jpa.repository.JpaRepository;

import cloud.fogbow.fs.core.plugins.PersistablePlanPlugin;

public interface FinancePlanRepository extends JpaRepository<PersistablePlanPlugin, String>{
}
