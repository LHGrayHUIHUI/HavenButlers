
package com.haven.storage.knowledge;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 文档向量类
 *
 * @author HavenButler
 */
@Data
public class DocumentVector {

    private String vectorId;
    private String documentId;
    private String chunkId;
    private List<Double> vector;
    private String content;
    private Map<String, Object> metadata;
}