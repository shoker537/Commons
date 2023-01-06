package ru.shk.commons.utils.items.universal.parse.type;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Getter@Accessors(fluent = true)@NoArgsConstructor
public abstract class Value <T> {
    private String name = null;
    private String stringValue = null;
    private T value = null;

    public Value<T> name(String name){
        this.name = name;
        return this;
    }

    public Value<T> stringValue(String stringValue){
        this.stringValue = stringValue;
        if(stringValue==null) {
            value = null;
            return this;
        }
        value = convertToObject(stringValue);
        return this;
    }

    public Value<T> value(T value){
        this.value = value;
        if(value==null) {
            stringValue = null;
            return this;
        }
        this.stringValue = convertToString(value);
        return this;
    }

    public abstract String convertToString(T object);

    public abstract T convertToObject(String s);
}
