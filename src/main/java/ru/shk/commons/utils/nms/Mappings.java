package ru.shk.commons.utils.nms;

import lombok.Getter;
import lombok.experimental.Accessors;
import ru.shk.commons.utils.nms.mappings.entries.Field;
import ru.shk.commons.utils.nms.mappings.entries.Method;

@Accessors(fluent = true)
public class Mappings {
    @Getter private static final Field Field = new Field();
    @Getter private static final Method Method = new Method();

}
