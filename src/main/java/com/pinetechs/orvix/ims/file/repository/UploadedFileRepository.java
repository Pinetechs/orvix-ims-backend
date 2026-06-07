package com.pinetechs.orvix.ims.file.repository;

import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    List<UploadedFile> findByIdInAndDeletedFalse(List<Long> ids);
}
