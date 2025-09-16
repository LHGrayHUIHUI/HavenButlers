package com.haven.storage.knowledge;

import lombok.Data;
import java.util.List;

/**
 * 添加文档请求
 */
@Data
public class AddDocumentRequest {
    private String title;
    private String content;
    private String sourceUrl;
    private String fileId;
    private String userId;
    private List<String> tags;
}