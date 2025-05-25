package com.tencent.supersonic.headless.core.pojo;

import lombok.Data;

@Data
public class SqlQuery {
    private String sql; // s2sql or s2sql(bizName)
    private String table; // t_datasetId
    private boolean supportWith = true;
    private boolean withAlias = true;
    private String simplifiedSql;
}
