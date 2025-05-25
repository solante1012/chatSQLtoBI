package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import lombok.Data;

import java.util.List;

// 代表请求 semantic model
@Data
public class ModelBuildReq {

    private String name;

    private String bizName;
    // 数据库标识
    private Long databaseId;
    // 域标识
    private Long domainId;

    private String sql;

    private String filterSql;

    private String catalog;

    private String db;

    private List<String> tables;

    // 表示数据库表的物理结构
    private List<DbSchema> dbSchemas;

    private boolean buildByLLM;

    private Integer chatModelId;

    private ChatModelConfig chatModelConfig;
}
