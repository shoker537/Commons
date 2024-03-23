package ru.shk.commons.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.SneakyThrows;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;

public class HTTPRequest {
    private final URL url;
    private String result;
    private static final Gson gson = new Gson();
    private int timeout = 5;
    private final HashMap<String, String> headers = new HashMap<>();

    public HTTPRequest timeout(int seconds){
        this.timeout = seconds;
        return this;
    }

    @SneakyThrows
    public HTTPRequest(String url){
        this.url = new URL(url);
    }

    public HTTPRequest(URL url){
        this.url = url;
    }

    public HTTPRequest post(JsonObject body){
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(timeout));
            headers.forEach(request::setHeader);
            HttpResponse<String> response = client.send(request.build(),
                    HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (Throwable t){
            t.printStackTrace();
        }
        return this;
    }

    public HTTPRequest header(String k, String v){
        headers.put(k, v);
        return this;
    }

    public HTTPRequest get(){
        HttpClient client;
        try {
            client = HttpClient.newHttpClient();
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .timeout(Duration.ofSeconds(timeout));
            headers.forEach(request::setHeader);
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (Throwable t){
            t.printStackTrace();
        }
//            HttpURLConnection c = (HttpURLConnection) url.openConnection();
//            c.connect();
//            try(val is = c.getInputStream()){
//                BufferedReader in = new BufferedReader(new InputStreamReader(is));
//                String inputLine;
//                StringBuilder response = new StringBuilder();
//                while ((inputLine = in.readLine()) != null) response.append(inputLine);
//                result = response.toString();
//            } finally {
//                c.disconnect();
//            }
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
