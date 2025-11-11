package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileBasicMetadata;
import com.haven.storage.domain.model.file.FileProcessingResult;
import okio.BufferedSource;

/**
 * 责任链的积累
 * 该责任链的职责是
 * 1.传入文件数据的处理
 * 2.校验
 * 3.对象的转化
 *
 */
interface FileProcessingStrategy {
    /**
     *
     * @param fileBasicMetadata   文件的描述
     * @param bufferedSource   文件的数据 输入流
     * @return
     */
    FileProcessingResult validate(FileBasicMetadata fileBasicMetadata, BufferedSource  bufferedSource);
}
