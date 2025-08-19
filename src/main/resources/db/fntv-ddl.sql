CREATE TABLE IF NOT EXISTS fn_media_info
(
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    media_name                VARCHAR(255),
    media_full_path           VARCHAR(255),
    media_guid                VARCHAR(255),
    media_type                VARCHAR(255),
    media_format              VARCHAR(20),
    duration                  INT8,
    category                  VARCHAR(20),
    m3u8_content              TEXT,
    create_by                 VARCHAR(255),
    update_by                 VARCHAR(255),
    create_time               TIMESTAMP,
    update_time               TIMESTAMP
);
COMMENT ON TABLE fn_media_info IS '飞牛媒体信息表';
COMMENT ON COLUMN fn_media_info.id IS '主键';
COMMENT ON COLUMN fn_media_info.media_name IS '媒体名称';
COMMENT ON COLUMN fn_media_info.media_full_path IS '媒体全路径';
COMMENT ON COLUMN fn_media_info.media_guid IS '媒体GUID';
COMMENT ON COLUMN fn_media_info.media_type IS '媒体类型';
COMMENT ON COLUMN fn_media_info.media_format IS '媒体格式';
COMMENT ON COLUMN fn_media_info.category IS '媒体分类';
COMMENT ON COLUMN fn_media_info.m3u8_content IS 'm3u8文件内容';
COMMENT ON COLUMN fn_media_info.create_by IS '创建人';
COMMENT ON COLUMN fn_media_info.update_by IS '修改人';
COMMENT ON COLUMN fn_media_info.create_time IS '创建时间';
COMMENT ON COLUMN fn_media_info.update_time IS '修改时间';

CREATE TABLE IF NOT EXISTS user_info
(
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name                 VARCHAR(255),
    cookie                    VARCHAR(255),
    is_admin                  BOOLEAN,
    create_by                 VARCHAR(255),
    update_by                 VARCHAR(255),
    create_time               TIMESTAMP,
    update_time               TIMESTAMP
);
COMMENT ON TABLE user_info IS '用户信息表';
COMMENT ON COLUMN user_info.id IS '主键';
COMMENT ON COLUMN user_info.user_name IS '用户名';
COMMENT ON COLUMN user_info.cookie IS 'cookie';
COMMENT ON COLUMN user_info.is_admin IS '是否管理员';
COMMENT ON COLUMN user_info.create_by IS '创建人';
COMMENT ON COLUMN user_info.update_by IS '修改人';
COMMENT ON COLUMN user_info.create_time IS '创建时间';
COMMENT ON COLUMN user_info.update_time IS '修改时间';
