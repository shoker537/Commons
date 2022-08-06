package ru.shk.commons.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.SneakyThrows;
import lombok.val;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPRequest {
    private final URL url;
    private String result;
    private final Gson gson = new Gson();

    @SneakyThrows
    public HTTPRequest(String url){
        this.url = new URL(url);
    }

    public HTTPRequest(URL url){
        this.url = url;
    }

    public HTTPRequest get(){
        try {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.connect();
            try(val is = c.getInputStream()){
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) response.append(inputLine);
                result = response.toString();
            } finally {
                c.disconnect();
            }
        } catch (Throwable t){
            t.printStackTrace();
            return null;
        }
        return this;
    }

    public JsonObject asJson(){
        return gson.fromJson(result, JsonObject.class);
    }

    public boolean isJson(){
        try {
            gson.fromJson(result, JsonObject.class);
            return true;
        } catch (JsonSyntaxException e){
            return false;
        }
    }

    public String asString(){
        return result;
    }
}
