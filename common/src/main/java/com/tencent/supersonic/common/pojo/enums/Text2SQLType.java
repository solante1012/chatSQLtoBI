package com.tencent.supersonic.common.pojo.enums;

// NONE 不用修正
public enum Text2SQLType {
    ONLY_RULE, LLM_OR_RULE, NONE;

    public boolean enableLLM() {
        return this.equals(LLM_OR_RULE);
    }
}
