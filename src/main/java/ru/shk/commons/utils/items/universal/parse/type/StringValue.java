package ru.shk.commons.utils.items.universal.parse.type;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter@Accessors(fluent = true)
public class StringValue extends Value<String> {

    @Override
    public String value(){
        return stringValue();
    }

    @Override
    public String convertToString(String object) {
        return object;
    }

    @Override
    public String convertToObject(String s) {
        return s;
    }
}
