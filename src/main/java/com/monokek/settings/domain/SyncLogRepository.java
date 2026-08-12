package com.monokek.settings.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface SyncLogRepository extends Repository<SyncLog, Long> {

    SyncLog save(SyncLog syncLog);
}
