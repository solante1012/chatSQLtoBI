package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
// 抽象语法树 中间转换结构视图类 ，通过build() 方法生成最终的抽象语法树 SqlNode
@Data
public class TableView {
        // 维度 或者 指标 字段
    private Set<String> fields = Sets.newHashSet();
    /**
     0 = {SqlIdentifier@24062} "src1_ai_target_yg_date.target_name"
     1 = {SqlIdentifier@24063} "src1_ai_target_yg_date.target_value"
     2 = {SqlIdentifier@24064} "src1_ai_target_yg_date.account_period"
     3 = {SqlIdentifier@24065} "src1_ai_org.org_byname"
     */
    private List<SqlNode> select = Lists.newArrayList(); // 抽象语法树 select节点
    private SqlNodeList order;
    private SqlNode fetch;
    private SqlNode offset;
    /**
     * SELECT *
     * FROM (SELECT *
     * FROM `ai_target_rc_date`) AS `src1_ai_target_rc_date`
     * LEFT JOIN (SELECT *
     * FROM `ai_org`) AS `src1_ai_org` ON `src1_ai_target_rc_date`.`org_id` = `src1_ai_org`.`org_id`
     * LEFT JOIN (SELECT *
     * FROM `ai_target_yg_date`) AS `src1_ai_target_yg_date` ON `src1_ai_org`.`org_id` = `src1_ai_target_yg_date`.`org_id`
     */
    private SqlNode table; // 抽象语法树 表节点
    private String alias;  // 表别名
    private List<String> primary; // 主/外建
    private ModelResp dataModel;

    public SqlNode build() {
        List<SqlNode> selectNodeList = new ArrayList<>();
        if (select.isEmpty()) {
            return new SqlSelect(SqlParserPos.ZERO, null,
                    new SqlNodeList(SqlNodeList.SINGLETON_STAR, SqlParserPos.ZERO), table, null,
                    null, null, null, null, order, offset, fetch, null);
        } else {
            selectNodeList.addAll(select);
            return new SqlSelect(SqlParserPos.ZERO, null,
                    new SqlNodeList(selectNodeList, SqlParserPos.ZERO), table, null, null, null,
                    null, null, order, offset, fetch, null);
        }
    }

}
