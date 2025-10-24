package ru.shk.commons.utils.items.converter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// === Value Types ===
sealed interface Value permits StringValue, NumberValue, ArrayValue, MapValue {
    String asString();
}

// === Parser ===
class ValueParser {
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(\\w+):" + // key
                    "(\\[.*?\\]|\".*?\"|''.*?''|\\S+)" // value: array/map | quoted | double-quoted | word
    );

    public static Map<String, Value> parse(String input) {
        Map<String, Value> map = new LinkedHashMap<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);

        while (matcher.find()) {
            String key = matcher.group(1);
            String rawValue = matcher.group(2).trim();
            map.put(key, parseValue(rawValue));
        }
        return map;
    }

    private static Value parseValue(String raw) {
        if (raw.startsWith("[") && raw.endsWith("]")) {
            return parseArrayOrMap(raw.substring(1, raw.length() - 1));
        } else if ((raw.startsWith("\"") && raw.endsWith("\"")) ||
                (raw.startsWith("''") && raw.endsWith("''"))) {
            String unquoted = raw.replaceAll("^(\"|''|')|(\"|''|')$", "");
            return new StringValue(unquoted);
        } else if (raw.matches("-?\\d+(\\.\\d+)?")) {
            if (raw.contains(".")) {
                return new NumberValue(Double.parseDouble(raw));
            } else {
                return new NumberValue(Long.parseLong(raw));
            }
        } else {
            return new StringValue(raw);
        }
    }

    private static Value parseArrayOrMap(String body) {
        List<String> parts = splitCommaSafe(body);
        boolean isMap = parts.stream().allMatch(p -> p.contains(":"));

        if (isMap) {
            Map<String, Value> map = new LinkedHashMap<>();
            for (String part : parts) {
                String[] kv = part.split(":", 2);
                String key = kv[0].trim();
                String val = kv[1].trim();
                map.put(key, parseValue(val));
            }
            return new MapValue(map);
        } else {
            List<Value> values = new ArrayList<>();
            for (String part : parts) {
                values.add(parseValue(part.trim()));
            }
            return new ArrayValue(values);
        }
    }

    private static List<String> splitCommaSafe(String body) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '[') depth++;
                if (c == ']') depth--;
            }
            if (c == ',' && depth == 0 && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }
}

// === Universal Converter ===
public class UniversalConverter<B> {
    private final List<StringConverterRule<B>> rules = new ArrayList<>();

    public void addRule(StringConverterRule<B> rule) {
        rules.add(rule);
    }

    public B fromString(String text, Supplier<B> builderSupplier) {
        B builder = builderSupplier.get();
        Map<String, Value> parsed = ValueParser.parse(text);
        for (StringConverterRule<B> rule : rules) {
            Value v = parsed.get(rule.key);
            if (v != null) {
                rule.setter.accept(builder, v);
            }
        }
        return builder;
    }

    public String toString(B builder) {
        StringBuilder sb = new StringBuilder();
        for (StringConverterRule<B> rule : rules) {
            Value v = rule.getter.apply(builder);
            if (v != null) {
                sb.append(rule.key).append(":").append(serializeValue(v)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String serializeValue(Value v) {
        return switch (v) {
            case StringValue sv -> (sv.value().contains(" ") ? "\"" + sv.value() + "\"" : sv.value());
            case NumberValue nv -> nv.value().toString();
            case ArrayValue av -> "[" + av.values().stream().map(this::serializeValue).reduce((a, b) -> a + "," + b).orElse("") + "]";
            case MapValue mv -> "[" + mv.map().entrySet().stream()
                    .map(e -> e.getKey() + ":" + serializeValue(e.getValue()))
                    .reduce((a, b) -> a + "," + b).orElse("") + "]";
        };
    }
}