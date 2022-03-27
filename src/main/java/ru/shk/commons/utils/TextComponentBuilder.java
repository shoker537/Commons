package ru.shk.commons.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class TextComponentBuilder {
    private final TextComponent component;

    public TextComponentBuilder(String text){
        component = new TextComponent(text);
    }

    public TextComponentBuilder withColor(ChatColor color){
        component.setColor(color);
        return this;
    }

    public TextComponentBuilder withHover(String hover){
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
        return this;
    }

    public TextComponentBuilder withCommand(String cmd){
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        return this;
    }

    public TextComponentBuilder withURL(String url){
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        return this;
    }

    public TextComponent build(){
        return component;
    }

}
