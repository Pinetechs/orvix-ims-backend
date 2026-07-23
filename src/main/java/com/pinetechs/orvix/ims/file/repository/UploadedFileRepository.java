package com.pinetechs.orvix.ims.file.repository;

import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.enums.UploadedFileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByIdInAndDeletedFalse(List<Long> ids);

    Optional<UploadedFile> findByIdAndDeletedFalse(Long id);

    /**
     * Attaches a file without loading it through the current transaction's
     * repeatable-read snapshot.
     *
     * <p>Scan images are created in a REQUIRES_NEW transaction. A surrounding
     * scan/recheck transaction may therefore have started before the file row
     * existed. MySQL executes this update as a current read, so it can see the
     * committed row while keeping the temp flag change part of the surrounding
     * transaction.</p>
     */
    @Modifying
    @Query("""
            update UploadedFile file
               set file.temp = false
             where file.id = :id
               and file.deleted = false
            """)
    int markAsAttached(@Param("id") Long id);

    List<UploadedFile> findByFileTypeAndTempTrueAndDeletedFalseAndCreatedAtBefore(
            UploadedFileType fileType,
            LocalDateTime cutoff
    );
}
