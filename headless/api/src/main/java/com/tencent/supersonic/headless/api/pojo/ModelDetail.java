package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelDetail {

    private String queryType;

    private String dbType;
 // 模型定义的sql  select * from ai_target_rc_date
    private String sqlQuery;

    private String tableQuery;

    private String filterSql;
        //  字段 （唯一标识 或 外键）
    private List<Identify> identifiers = Lists.newArrayList();
        // 对数据进行分类或过滤的属性（例如，日期、部门、页面）
    private List<Dimension> dimensions = Lists.newArrayList();
        // 可以聚合的数值（例如，页面浏览量、停留时间）
    private List<Measure> measures = Lists.newArrayList();

    private List<Field> fields = Lists.newArrayList();

    private List<SqlVariable> sqlVariables = Lists.newArrayList();

    public String getSqlQuery() {
        if (StringUtils.isNotBlank(sqlQuery) && sqlQuery.endsWith(";")) {
            sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 1);
        }
        return sqlQuery;
    }

    public List<Dimension> filterTimeDims() {
        if (CollectionUtils.isEmpty(dimensions)) {
            return Lists.newArrayList();
        }
        return dimensions.stream().filter(dim -> DimensionType.partition_time.equals(dim.getType()))
                .collect(Collectors.toList());
    }

}
