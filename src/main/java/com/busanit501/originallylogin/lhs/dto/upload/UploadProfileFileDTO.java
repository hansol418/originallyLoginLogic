package com.busanit501.originallylogin.lhs.dto.upload;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadProfileFileDTO {
    private MultipartFile file;
}
