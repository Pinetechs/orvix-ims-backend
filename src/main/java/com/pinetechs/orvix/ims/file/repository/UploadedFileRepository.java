package com.pinetechs.orvix.ims.file.repository;

import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByIdInAndDeletedFalse(List<Long> ids);

    Optional<UploadedFile> findByIdAndDeletedFalse(Long id);
}
