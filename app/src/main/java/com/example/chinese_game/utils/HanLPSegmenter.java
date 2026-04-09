package com.example.chinese_game.utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 HanLP 进行中文分词，并将词性转换为 CTB (Chinese Penn Treebank) 标注。
 * 可将句子切分为词列表，并可选择是否包含词性。
 */
public final class HanLPSegmenter {

    private static final Map<String, String> HANLP_TO_CTB = new HashMap<>();

    static {
        // 名词类 n* → NN；专名 → NR；时间 → NT
        HANLP_TO_CTB.put("n", "NN");
        HANLP_TO_CTB.put("nr", "NR");   // 人名
        HANLP_TO_CTB.put("ns", "NR");   // 地名
        HANLP_TO_CTB.put("nt", "NR");   // 机构团体
        HANLP_TO_CTB.put("nz", "NR");   // 其他专名
        HANLP_TO_CTB.put("nl", "NN");
        HANLP_TO_CTB.put("t", "NT");    // 时间词
        HANLP_TO_CTB.put("tg", "NT");   // 时间语素
        // 动词类 v* → VV；系词/有 → VC/VE
        HANLP_TO_CTB.put("v", "VV");
        HANLP_TO_CTB.put("vn", "VV");
        HANLP_TO_CTB.put("vi", "VV");
        HANLP_TO_CTB.put("vl", "VV");
        HANLP_TO_CTB.put("vg", "VV");
        HANLP_TO_CTB.put("vd", "VV");
        HANLP_TO_CTB.put("vf", "VV");
        HANLP_TO_CTB.put("vx", "VV");
        HANLP_TO_CTB.put("vc", "VC");   // 是、为
        HANLP_TO_CTB.put("ve", "VE");   // 有
        // 形容词 a* → VA(谓语) / JJ(定语)
        HANLP_TO_CTB.put("a", "VA");
        HANLP_TO_CTB.put("ad", "AD");   // 副形
        HANLP_TO_CTB.put("an", "JJ");   // 名形
        HANLP_TO_CTB.put("ag", "VA");
        // 副词 d* → AD
        HANLP_TO_CTB.put("d", "AD");
        HANLP_TO_CTB.put("df", "AD");
        HANLP_TO_CTB.put("dg", "AD");
        // 代词 r* → PN
        HANLP_TO_CTB.put("r", "PN");
        HANLP_TO_CTB.put("rr", "PN");
        HANLP_TO_CTB.put("ry", "PN");
        HANLP_TO_CTB.put("rz", "PN");
        HANLP_TO_CTB.put("rv", "PN");
        // 数词、量词
        HANLP_TO_CTB.put("m", "CD");
        HANLP_TO_CTB.put("mq", "CD");
        HANLP_TO_CTB.put("q", "M");
        HANLP_TO_CTB.put("qv", "M");
        HANLP_TO_CTB.put("qt", "M");
        HANLP_TO_CTB.put("od", "OD");   // 序数
        // 介词、连词
        HANLP_TO_CTB.put("p", "P");
        HANLP_TO_CTB.put("c", "CC");
        HANLP_TO_CTB.put("cc", "CC");
        HANLP_TO_CTB.put("cs", "CS");
        // 助词 u* → 按常见字细分
        HANLP_TO_CTB.put("u", "DEG");
        HANLP_TO_CTB.put("ud", "DEG");  // 的
        HANLP_TO_CTB.put("ug", "DEG");
        HANLP_TO_CTB.put("uj", "DEG");
        HANLP_TO_CTB.put("ul", "AS");   // 了
        HANLP_TO_CTB.put("uv", "DEV");  // 地
        HANLP_TO_CTB.put("uz", "AS");   // 着
        HANLP_TO_CTB.put("y", "SP");    // 语气词 吗/呢/吧
        // 标点 w* → PU
        HANLP_TO_CTB.put("w", "PU");
        HANLP_TO_CTB.put("wd", "PU");
        HANLP_TO_CTB.put("wf", "PU");
        HANLP_TO_CTB.put("wj", "PU");
        HANLP_TO_CTB.put("wn", "PU");
        HANLP_TO_CTB.put("wp", "PU");
        HANLP_TO_CTB.put("ws", "PU");
        HANLP_TO_CTB.put("wb", "PU");
        HANLP_TO_CTB.put("wh", "PU");
        HANLP_TO_CTB.put("wm", "PU");
        HANLP_TO_CTB.put("wq", "PU");
        HANLP_TO_CTB.put("ww", "PU");
        // 其他
        HANLP_TO_CTB.put("e", "IJ");    // 叹词
        HANLP_TO_CTB.put("o", "ON");    // 拟声
        HANLP_TO_CTB.put("f", "LC");    // 方位词
        HANLP_TO_CTB.put("b", "JJ");    // 区别词
        HANLP_TO_CTB.put("i", "NN");    // 成语
        HANLP_TO_CTB.put("l", "NN");    // 习语
        HANLP_TO_CTB.put("j", "NN");    // 缩略
        HANLP_TO_CTB.put("g", "X");     // 语素
        HANLP_TO_CTB.put("h", "X");     // 前接成分
        HANLP_TO_CTB.put("k", "X");     // 后接成分
        HANLP_TO_CTB.put("x", "X");     // 其他/非语素
        HANLP_TO_CTB.put("xx", "X");
        HANLP_TO_CTB.put("z", "VA");    // 状态词
        HANLP_TO_CTB.put("ba", "BA");   // 把
        HANLP_TO_CTB.put("lb", "LB");   // 被(长)
        HANLP_TO_CTB.put("sb", "SB");   // 被(短)
        HANLP_TO_CTB.put("dec", "DEC");
        HANLP_TO_CTB.put("deg", "DEG");
        HANLP_TO_CTB.put("der", "DER");
        HANLP_TO_CTB.put("dev", "DEV");
        HANLP_TO_CTB.put("msp", "MSP");
        HANLP_TO_CTB.put("dt", "DT");
        HANLP_TO_CTB.put("etc", "ETC");
    }

    private HanLPSegmenter() {}

    /**
     * 将 HanLP 内部词性 (nature) 转换为 CTB (Chinese Penn Treebank) 词性标注。
     *
     * @param hanlpNature HanLP 返回的 nature 字符串，如 n, nr, v, ns
     * @return CTB 标签，如 NN, NR, VV；未知或空则返回 "X"
     */
    public static String hanlpNatureToCTB(String hanlpNature) {
        if (hanlpNature == null || hanlpNature.isEmpty()) return "X";
        String key = hanlpNature.trim().toLowerCase();
        String ctb = HANLP_TO_CTB.get(key);
        if (ctb != null) return ctb;
        // 首字母兜底
        if (key.startsWith("n")) return "NN";
        if (key.startsWith("v")) return "VV";
        if (key.startsWith("a")) return "VA";
        if (key.startsWith("d")) return "AD";
        if (key.startsWith("r")) return "PN";
        if (key.startsWith("m") || key.startsWith("q")) return "CD";
        if (key.startsWith("p")) return "P";
        if (key.startsWith("c")) return "CC";
        if (key.startsWith("u")) return "DEG";
        if (key.startsWith("w")) return "PU";
        return "X";
    }

    /**
     * 对句子进行分词，返回词语列表（不包含词性）。
     *
     * @param sentence 待分词的中文句子，如 "我喜欢在北京学习中文"
     * @return 词语列表，如 ["我", "喜欢", "在", "北京", "学习", "中文"]；若输入为 null 或空则返回空列表
     */
    public static List<String> segment(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Term> terms = HanLP.segment(sentence.trim());
        List<String> words = new ArrayList<>(terms.size());
        for (Term term : terms) {
            words.add(term.word);
        }
        return words;
    }

    /**
     * 对句子进行分词，返回带词性的词信息列表（词性已转换为 CTB 标注，可用于 sentence_words 的 pos_tag）。
     *
     * @param sentence 待分词的中文句子
     * @return 按顺序的词语与 CTB 词性列表；若输入为 null 或空则返回空列表
     */
    public static List<SegmentResult> segmentWithPos(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Term> terms = HanLP.segment(sentence.trim());
        List<SegmentResult> results = new ArrayList<>(terms.size());
        for (Term term : terms) {
            String natureStr = term.nature != null ? term.nature.toString() : "";
            String ctbPos = hanlpNatureToCTB(natureStr);
            results.add(new SegmentResult(term.word, ctbPos));
        }
        return results;
    }

    /**
     * 分词结果：词语 + 词性（CTB 标注，如 NN, VV, NR, PN 等）。
     */
    public static class SegmentResult {
        private final String word;
        private final String posTag;

        public SegmentResult(String word, String posTag) {
            this.word = word;
            this.posTag = posTag;
        }

        public String getWord() {
            return word;
        }

        public String getPosTag() {
            return posTag;
        }

        @Override
        public String toString() {
            return word + "/" + posTag;
        }
    }
}
