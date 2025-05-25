package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.enums.ChatWorkflowState;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.corrector.SemanticCorrector;
import com.tencent.supersonic.headless.chat.mapper.SchemaMapper;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatWorkflowEngine {

    private final List<SchemaMapper> schemaMappers = CoreComponentFactory.getSchemaMappers();
    private final List<SemanticParser> semanticParsers = CoreComponentFactory.getSemanticParsers();
    private final List<SemanticCorrector> semanticCorrectors =
            CoreComponentFactory.getSemanticCorrectors();

    // 核心工作流入口
    public void start(ChatWorkflowState initialState, ChatQueryContext queryCtx) {
        ParseResp parseResult = queryCtx.getParseResp();
        queryCtx.setChatWorkflowState(initialState);
        while (queryCtx.getChatWorkflowState() != ChatWorkflowState.FINISHED) {
            switch (queryCtx.getChatWorkflowState()) {
                // Schema 映射  通过将查询文本与知识库匹配来识别对用户查询中模式元素（指标、维度、实体、值）
                // 系统识别对自然语言查询中模式元素的引用，将术语映射到模型、维度、指标和值。
                case MAPPING:
                    //    com.tencent.supersonic.headless.chat.mapper.EmbeddingMapper, \
                    //    com.tencent.supersonic.headless.chat.mapper.KeywordMapper, \
                    //    com.tencent.supersonic.headless.chat.mapper.QueryFilterMapper, \
                    //    com.tencent.supersonic.headless.chat.mapper.PartitionTimeMapper,\
                    //    com.tencent.supersonic.headless.chat.mapper.TermDescMapper,\
                    //    com.tencent.supersonic.headless.chat.mapper.AllFieldMapper
                    performMapping(queryCtx);
                    if (queryCtx.getMapInfo().isEmpty()) {
                        parseResult.setState(ParseResp.ParseState.FAILED);
                        parseResult.setErrorMsg(
                                "No semantic entities can be mapped against user question.");
                        queryCtx.setChatWorkflowState(ChatWorkflowState.FINISHED);
                    } else {
                        queryCtx.setChatWorkflowState(ChatWorkflowState.PARSING);
                    }
                    break;
                    // 解析 理解用户查询并生成语义查询语句（S2SQL）
                // 基于映射的模式元素和查询意图，系统生成结构化语义查询表示（S2SQL）。
//                com.tencent.supersonic.headless.chat.parser.llm.LLMSqlParser,\
//                com.tencent.supersonic.headless.chat.parser.rule.RuleSqlParser,\
//                com.tencent.supersonic.headless.chat.parser.QueryTypeParser
                case PARSING:
                    performParsing(queryCtx);
                    if (queryCtx.getCandidateQueries().isEmpty()) {
                        parseResult.setState(ParseResp.ParseState.FAILED);
                        parseResult.setErrorMsg("No semantic queries can be parsed out.");
                        queryCtx.setChatWorkflowState(ChatWorkflowState.FINISHED);
                    } else {
                        List<SemanticParseInfo> parseInfos = queryCtx.getCandidateQueries().stream()
                                .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
                        parseResult.setSelectedParses(parseInfos);
                        if (queryCtx.needSQL()) {
                            queryCtx.setChatWorkflowState(ChatWorkflowState.S2SQL_CORRECTING);
                        } else {
                            parseResult.setState(ParseResp.ParseState.COMPLETED);
                            queryCtx.setChatWorkflowState(ChatWorkflowState.FINISHED);
                        }
                    }
                    break;
                    // 修正 检查语义查询语句的有效性，并在必要时通过基于规则和基于LLM的机制执行更正。
                //    com.tencent.supersonic.headless.chat.corrector.RuleSqlCorrector,\
                //    com.tencent.supersonic.headless.chat.corrector.LLMSqlCorrector
                case S2SQL_CORRECTING:
                    performCorrecting(queryCtx);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.TRANSLATING);
                    break;
                    // 通过 S2SemanticLayerService 生成 SQL  将语义查询语句（S2SQL）转换为可针对物理数据模型运行的可执行SQL。
                // 1. 解决模型关系和连接
                // 2. 为指标应用适当的聚合
                // 3. 处理过滤器和约束
                // 4. 实现基于时间的比较
                case TRANSLATING:
                    long start = System.currentTimeMillis();
                    performTranslating(queryCtx, parseResult);
                    parseResult.getParseTimeCost().setSqlTime(System.currentTimeMillis() - start);
                    queryCtx.setChatWorkflowState(ChatWorkflowState.FINISHED);
                    break;
                default:
                    if (parseResult.getState().equals(ParseResp.ParseState.PENDING)) {
                        parseResult.setState(ParseResp.ParseState.COMPLETED);
                    }
                    queryCtx.setChatWorkflowState(ChatWorkflowState.FINISHED);
                    break;
            }
        }
    }

    private void performMapping(ChatQueryContext queryCtx) {
        if (Objects.isNull(queryCtx.getMapInfo())
                || MapUtils.isEmpty(queryCtx.getMapInfo().getDataSetElementMatches())) {
            schemaMappers.forEach(mapper -> mapper.map(queryCtx));
        }
    }

    private void performParsing(ChatQueryContext queryCtx) {
        semanticParsers.forEach(parser -> {
            parser.parse(queryCtx);
            log.debug("{} result:{}", parser.getClass().getSimpleName(),
                    JsonUtil.toString(queryCtx));
        });
    }

    private void performCorrecting(ChatQueryContext queryCtx) {
        List<SemanticQuery> candidateQueries = queryCtx.getCandidateQueries();
        if (CollectionUtils.isNotEmpty(candidateQueries)) {
            for (SemanticQuery semanticQuery : candidateQueries) {
                for (SemanticCorrector corrector : semanticCorrectors) {
                    corrector.correct(queryCtx, semanticQuery.getParseInfo());
                    if (!ChatWorkflowState.S2SQL_CORRECTING
                            .equals(queryCtx.getChatWorkflowState())) {
                        break;
                    }
                }
            }
        }
    }

    private void performTranslating(ChatQueryContext queryCtx, ParseResp parseResult) {
        List<SemanticParseInfo> semanticParseInfos = queryCtx.getCandidateQueries().stream()
                .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
        List<String> errorMsg = new ArrayList<>();
        if (StringUtils.isNotBlank(parseResult.getErrorMsg())) {
            errorMsg.add(parseResult.getErrorMsg());
        }
        semanticParseInfos.forEach(parseInfo -> {
            try {
                SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
                if (Objects.isNull(semanticQuery)) {
                    return;
                }
                semanticQuery.setParseInfo(parseInfo);
                SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
                SemanticLayerService queryService =
                        ContextUtils.getBean(SemanticLayerService.class);
                SemanticTranslateResp explain =
                        queryService.translate(semanticQueryReq, queryCtx.getRequest().getUser());
                if (explain.isOk()) {
                    parseInfo.getSqlInfo().setQuerySQL(explain.getQuerySQL());
                    parseResult.setState(ParseResp.ParseState.COMPLETED);
                } else {
                    parseResult.setState(ParseResp.ParseState.FAILED);
                }
                if (StringUtils.isNotBlank(explain.getErrMsg())) {
                    errorMsg.add(explain.getErrMsg());
                }
                log.info(
                        "SqlInfoProcessor results:\n"
                                + "Parsed S2SQL: {}\nCorrected S2SQL: {}\nQuery SQL: {}",
                        StringUtils.normalizeSpace(parseInfo.getSqlInfo().getParsedS2SQL()),
                        StringUtils.normalizeSpace(parseInfo.getSqlInfo().getCorrectedS2SQL()),
                        StringUtils.normalizeSpace(parseInfo.getSqlInfo().getQuerySQL()));
            } catch (Exception e) {
                log.warn("get sql info failed:{}", e);
                errorMsg.add(String.format("S2SQL:%s %s", parseInfo.getSqlInfo().getParsedS2SQL(),
                        e.getMessage()));
            }
        });
        if (!errorMsg.isEmpty()) {
            parseResult.setErrorMsg(String.join("\n", errorMsg));
        }
    }
}
