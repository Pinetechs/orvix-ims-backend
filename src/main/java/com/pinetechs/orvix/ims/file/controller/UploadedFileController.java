package com.pinetechs.orvix.ims.file.controller;

import com.pinetechs.orvix.ims.file.dto.UploadedFileResponse;
import com.pinetechs.orvix.ims.file.entity.UploadedFile;
import com.pinetechs.orvix.ims.file.service.UploadedFileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class UploadedFileController {

    private final UploadedFileService uploadedFileService;

    public UploadedFileController(UploadedFileService uploadedFileService) {
        this.uploadedFileService = uploadedFileService;
    }

    @PostMapping("/upload")
    public UploadedFileResponse upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(name = "folder", required = false) String folder,
                                       @RequestParam(name = "temp", defaultValue = "true") boolean temp) throws IOException {
        UploadedFile uploadedFile = uploadedFileService.upload(file, folder, temp);
        return UploadedFileResponse.from(uploadedFile);
    }
}
