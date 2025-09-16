package com.haven.storage.file;

import lombok.Data;
import java.util.List;

/**
 * 家庭文件列表
 */
@Data
public class FamilyFileList {
    private String familyId;
    private String currentPath;
    private List<FileMetadata> files;
    private List<String> subFolders;
    private int totalFiles;
    private long totalSize;
    private String traceId;
}