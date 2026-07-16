package com.pinetechs.orvix.ims.jobs.repository;

import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {

    @Query("""
            select j.id
            from BackgroundJob j
            where j.status = :status
              and (j.scheduledTime is null or j.scheduledTime <= :now)
            order by
              case j.priority
                when com.pinetechs.orvix.ims.jobs.enums.JobPriority.URGENT then 4
                when com.pinetechs.orvix.ims.jobs.enums.JobPriority.HIGH then 3
                when com.pinetechs.orvix.ims.jobs.enums.JobPriority.NORMAL then 2
                when com.pinetechs.orvix.ims.jobs.enums.JobPriority.LOW then 1
                else 0
              end desc,
              j.createdAt asc
            """)
    List<Long> findCandidateIds(
            @Param("status") JobStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update BackgroundJob j
            set j.status = :newStatus,
                j.startedAt = current_timestamp,
                j.progress = 5,
                j.message = 'Job claimed by worker'
            where j.id in :ids
              and j.status = :currentStatus
            """)
    int claimJob(
            @Param("ids") List<Long> ids,
            @Param("currentStatus") JobStatus currentStatus,
            @Param("newStatus") JobStatus newStatus
    );

    List<BackgroundJob> findByIdInAndStatus(List<Long> ids, JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from BackgroundJob job where job.relatedId = :relatedId")
    List<BackgroundJob> findByRelatedIdForUpdate(@Param("relatedId") Long relatedId);
}
