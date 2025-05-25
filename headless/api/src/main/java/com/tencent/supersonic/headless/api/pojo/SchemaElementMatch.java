package com.tencent.supersonic.headless.api.pojo;

import lombok.*;

import java.io.Serializable;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaElementMatch implements Serializable {
    // 配匹到的语义元素
    private SchemaElement element;
    private double offset;
    private double similarity;
    // 原始词
    private String detectWord;
    // 匹配到的词
    private String word;
    private Long frequency;
    private boolean isInherited;
    private boolean llmMatched;

    public boolean isFullMatched() {
        return 1.0 == similarity;
    }
}
