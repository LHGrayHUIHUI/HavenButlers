package com.haven.storage.processor.context;


import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.FileStatus;
import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileBasicMetadata;
import com.haven.storage.domain.model.file.FileStorageInfo;
import lombok.Data;
import lombok.Getter;
import okio.BufferedSource;
import org.springframework.util.StringUtils;

/**
 * 在拦截器中作为数据支撑的上下文处理的
 */
@Data
public class FileProcessContext {
    private String traceId;
    private FileBasicMetadata fileBasicMetadata;//项目通用的元数据类型
    private FileStorageInfo fileStorageInfo;//项目中文件存储的信息的
    private FileOperation operationType;//当前操作是类型
    private StorageType storageType = StorageType.MINIO;//存储的方式默认是 minio服务存储

    /**
     * 文件状态（如正常、已删除、过期、审核中等）
     * 管理文件生命周期，控制文件是否可访问、是否需要清理
     */
    private FileStatus fileStatus;
    private ProcessingStage stage = ProcessingStage.INITIALIZED;//流程的状态是

    private String errorMessage;
    private BufferedSource bufferedSource;//文件的输入流的替换的是 MultipartFile对象的

    //是否是在校验文件的流
    private boolean validFileData = false;

    public String getTraceId() {
        if (StringUtils.hasLength(traceId)) return traceId;
        return TraceIdUtil.getCurrentOrGenerate();
    }

    @Getter
    public enum ProcessingStage {
        INITIALIZED("初始化", 0),              // 初始化
        VALIDATED("已验证", 1),               // 已验证
        METADATA_CREATED("元数据已创建", 2),        // 元数据已创建
        METADATA_QUERIED("元数据已查询", 3),        // 元数据已查询
        FILE_STORED("文件已存储", 4),            // 文件已存储
        FILE_DOWNLOADED("文件已下载", 5),        // 文件已下载
        METADATA_PERSISTED("元数据已持久化", 6),     // 元数据已持久化
        COMPLETED("完成", 7),              // 完成
        ROLLED_BACK("已回滚", 8);            // 已回滚

        private final String desc;
        private final int intValue;

        ProcessingStage(String desc, int intValue) {
            this.desc = desc;
            this.intValue = intValue;
        }
    }
}
