package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SparePartInventoryBranchRepository extends JpaRepository<SparePartInventoryBranch, Long> {

    Optional<SparePartInventoryBranch> findByInventoryTaskIdAndBranchName(Long taskId, String branchName);

    List<SparePartInventoryBranch> findByInventoryTaskId(Long taskId);

    boolean existsByInventoryTaskIdAndBranchName(Long taskId, String branchName);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryBranch branch
           where branch.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}
