package com.haven.storage.domain.model.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.haven.base.model.entity.BaseModel;
import com.haven.storage.domain.model.enums.FileCategory;
import com.haven.storage.domain.model.enums.FileVisibility;
import lombok.Data;
import okio.BufferedSource;

import java.time.LocalDateTime;

/**
 * 文件基础元数据模型
 * 作为项目中所有文件处理操作（上传、下载、权限控制、存储管理等）的基础数据载体，
 * 存储文件的核心属性，供全流程复用。
 * 在拓展的过程中需要继承这个类来进行实现的
 */
@Data
public abstract class FileBasicMetadata implements BaseModel {

    /**
     * 文件唯一标识（全局唯一）
     * 用于在系统中唯一定位一个文件（如数据库主键、缓存键等）
     */
    private String fileId;
    /**
     * 文件的家庭id的
     */
    private String familyId;

    /**
     * 文件名称（原始文件名，如"简历.pdf"）
     * 保留用户上传时的文件名，用于展示和下载
     */
    private String fileName;

    /**
     * 文件格式（扩展名，如"pdf"、"jpg"）
     * 用于区分文件类型细节，辅助格式校验、预览等功能
     */
    private String fileFormat;

    /**
     * 文件分类（如文档、图片、视频等宏观分类）
     * 用于文件归类管理，对应业务中的大类别划分
     */
    private FileCategory fileCategory;

    /**
     * 文件可见性（如私有、家庭可见、公开等）
     * 控制文件的访问权限范围，用于权限校验逻辑
     */
    private FileVisibility fileVisibility;

    /**
     * 文件大小（单位：字节）
     * 记录文件实际大小，用于存储容量计算、大小限制校验
     */
    private Long fileSize;

    /**
     * 文件拥有者ID（创建者/上传者的用户ID）
     * 标识文件的归属，用于权限判断（如所有者拥有全部操作权）
     */
    private String ownerId;

    /**
     * 存储路径（逻辑路径，如"/家庭相册/2024年"）
     * 用于文件在系统中的逻辑分类存储，方便用户浏览和管理
     */
    private String folderPath;

    /**
     * 物理存储标识（如MinIO的objectKey、本地存储的绝对路径）
     * 指向文件在实际存储介质中的位置，用于读取/删除物理文件
     */
    private String storageKey;

    /**
     * 文件描述（用户对文件的备注信息）
     * 可选字段，用于补充文件的额外说明
     */
    private String description;

    /**
     * 上传时间（文件首次上传到系统的时间）
     * 用于记录文件创建时间，支持按时间排序、筛选
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * 最后修改时间（文件属性或内容最后更新的时间）
     * 用于追踪文件变更记录，如重命名、修改描述后更新
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModifiedTime;



    /**
     * 获取文件的data数据
     *
     * @return
     */

    public abstract BufferedSource getFileData();

}
