package com.haven.storage.domain.model.file;

import lombok.Data;
import java.util.List;

/**
 * 文件搜索结果
 */
@Data
public class FileSearchResult {
    private String familyId;
    private String keyword;
    private List<FileMetadata> matchedFiles;
    private int totalMatches;
    private String traceId;
}