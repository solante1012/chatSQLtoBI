package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
/*
 * few-shot示例  每次召回的记忆数由系统参数来调整。
 * 系统冷启动阶段，会从静态文件s2-exemplar.json中导入内置示例，但不推荐用户直接修改内容。
 *
 * 从向量库召回的Text2SQLExemplar除了用于few-shot示例，也作为相似问题在界面推荐给用户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Text2SQLExemplar implements Serializable {

    public static final String PROPERTY_KEY = "sql_exemplar";

    private String question;

    private String sideInfo;

    private String dbSchema;

    private String sql;
}
