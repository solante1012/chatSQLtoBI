package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.SqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * This parser generates inner sql statement for the ontology query, which would be selected by the
 * parsed sql query.
 *
 * 有子查询sql ,构造关联关系
 */
@Component("OntologyQueryParser")
@Slf4j
public class OntologyQueryParser implements QueryParser {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getOntologyQuery());
    }

    // 转换sql
    @Override
    public void parse(QueryStatement queryStatement) throws Exception {
        Ontology ontology = queryStatement.getOntology();
        //rg.apache.calcite 是一个强大的 SQL 解析、优化和执行框架，常用于构建数据库中间件、查询引擎等。

        S2CalciteSchema semanticSchema = S2CalciteSchema.builder()
                .schemaKey("DATASET_" + queryStatement.getDataSetId()).ontology(ontology)
                .runtimeOptions(RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                        .enableOptimize(queryStatement.getEnableOptimize()).build())
                .build();
        SqlBuilder sqlBuilder = new SqlBuilder(semanticSchema);
        // 构造关联关系
        String sql = sqlBuilder.buildOntologySql(queryStatement);
        queryStatement.getOntologyQuery().setSql(sql);
    }

}
