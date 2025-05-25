package com.tencent.supersonic.headless.chat.knowledge;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.Constants;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class HanlpMapResult extends MapResult {
    private List<String> natures;

    /**
     *
     * @param name   匹配到的词 文本
     * @param natures 词性列表
     * @param detectWord  拿来匹配的 字符串
     * @param similarity  相似度
     */
    public HanlpMapResult(String name, List<String> natures, String detectWord, double similarity) {
        this.name = name;
        this.natures = natures;
        this.detectWord = detectWord;
        this.similarity = similarity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HanlpMapResult hanlpMapResult = (HanlpMapResult) o;
        return Objects.equal(name, hanlpMapResult.name)
                && Objects.equal(natures, hanlpMapResult.natures);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, natures);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public String getMapKey() {
        return this.getName() + Constants.UNDERLINE
                + String.join(Constants.UNDERLINE, this.getNatures());
    }
}
