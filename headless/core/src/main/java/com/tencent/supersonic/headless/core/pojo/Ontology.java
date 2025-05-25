package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An ontology comprises a group of data models that can be joined together either in star schema or
 * snowflake schema.
 * 一个 Ontology 是由一组数据模型组成的，这些模型可以通过 星型模式（star schema） 或 雪花模式（snowflake schema） 联合在一起。
 */
@Data
public class Ontology {

    private DatabaseResp database;
    private Map<String, ModelResp> modelMap = new HashMap<>();
    private Map<String, List<MetricSchemaResp>> metricMap = new HashMap<>(); // key 为模型名，value 为该模型下的指标列表
    private Map<String, List<DimSchemaResp>> dimensionMap = new HashMap<>(); // ，key 为模型名，value 为该模型下的维度列表
    private List<JoinRelation> joinRelations;  // 表之间的关联关系，定义如何 join 模型

    public List<MetricSchemaResp> getMetrics() {
        return metricMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<DimSchemaResp> getDimensions() {
        return dimensionMap.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public EngineType getDatabaseType() {
        if (Objects.nonNull(database)) {
            return EngineType.fromString(database.getType().toUpperCase());
        }
        return null;
    }

    public String getDatabaseVersion() {
        if (Objects.nonNull(database)) {
            return database.getVersion();
        }
        return null;
    }

}
