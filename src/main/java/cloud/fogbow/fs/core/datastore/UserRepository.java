package cloud.fogbow.fs.core.datastore;

import org.springframework.data.jpa.repository.JpaRepository;

import cloud.fogbow.fs.core.models.FinanceUser;

public interface UserRepository extends JpaRepository<FinanceUser, String>{
    FinanceUser findByIdAndProvider(String id, String provider);
}
