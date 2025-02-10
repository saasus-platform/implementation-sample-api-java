package implementsample.repository;

import implementsample.entity.DeleteUserLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeleteUserLogRepository extends JpaRepository<DeleteUserLog, Long> {
    List<DeleteUserLog> findByTenantId(String tenantId);
}
