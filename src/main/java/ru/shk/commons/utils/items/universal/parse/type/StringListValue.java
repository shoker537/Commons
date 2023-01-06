package ru.shk.commons.utils.items.universal.parse.type;

import java.util.ArrayList;
import java.util.List;

public class StringListValue extends Value<List<String>> {

    @Override
    public String convertToString(List<String> object) {
        StringBuilder sb = new StringBuilder("[");
        for (String s : object) sb.append("\"").append(s).append("\"");
        sb.append("]");
        return sb.toString();
    }

    @Override
    public List<String> convertToObject(String s) {
        if(!s.endsWith(",")) s+=",";
        List<String> list = new ArrayList<>();
        char[] chars = s.toCharArray();
        boolean isInsideTextBlock = false;
        boolean isNextCharEscaped = false;
        StringBuilder typing = new StringBuilder();
        for (char c : chars) {
            if(c=='\\' && !isNextCharEscaped){
                isNextCharEscaped = true;
                continue;
            }
            if(c=='\"' && !isNextCharEscaped){
                isInsideTextBlock = !isInsideTextBlock;
                continue;
            }
            if(!isInsideTextBlock && c==',' && !isNextCharEscaped){
                list.add(typing.toString());
                typing = new StringBuilder();
                continue;
            }
            typing.append(c);
            isNextCharEscaped = false;
        }
        return list;
    }
}
