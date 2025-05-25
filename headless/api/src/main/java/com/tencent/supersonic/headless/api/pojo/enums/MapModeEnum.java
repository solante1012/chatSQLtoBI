package com.tencent.supersonic.headless.api.pojo.enums;

public enum MapModeEnum {
    // 匹配从严格到宽泛逐步变化
    STRICT(0), MODERATE(2), LOOSE(4), ALL(6);

    public int threshold;

    MapModeEnum(Integer threshold) {
        this.threshold = threshold;
    }
}
