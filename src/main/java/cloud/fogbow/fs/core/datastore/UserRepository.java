package cloud.fogbow.fs.core.datastore;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserId;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<FinanceUser, String>{
    FinanceUser findByUserId(UserId userId);
}
