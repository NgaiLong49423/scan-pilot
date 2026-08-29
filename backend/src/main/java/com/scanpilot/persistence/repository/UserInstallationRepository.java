package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.UserInstallationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInstallationRepository extends JpaRepository<UserInstallationEntity, UUID>, UserInstallationRepositoryCustom {

    Optional<UserInstallationEntity> findByUserIdAndInstallationId(UUID userId, Long installationId);

    List<UserInstallationEntity> findByUserId(UUID userId);

    List<UserInstallationEntity> findByInstallationId(Long installationId);
}
