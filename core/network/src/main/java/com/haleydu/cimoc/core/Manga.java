package com.haleydu.cimoc.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Manga {

    public static String getResponseBody(OkHttpClient client, Request request) throws NetworkErrorException {
        return getResponseBody(client, request, true);
    }

    private static String getResponseBody(OkHttpClient client, Request request, boolean retry) throws NetworkErrorException {
        if (client == null || request == null) {
            throw new NetworkErrorException();
        }
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                byte[] bodybytes = response.body().bytes();
                String body = new String(bodybytes);
                Matcher m = Pattern.compile("charset=([\\w\\-]+)").matcher(body);
                if (m.find()) {
                    body = new String(bodybytes, m.group(1));
                }
                return body;
            } else if (retry)
                return getResponseBody(client, nextFallbackRequest(request), false);
        } catch (Exception e) {
            e.printStackTrace();
            if (retry)
                return getResponseBody(client, nextFallbackRequest(request), false);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        Request fallback = nextFallbackRequest(request);
        if (fallback != request) {
            return getResponseBody(client, fallback, true);
        }
        throw new NetworkErrorException();
    }

    private static Request nextFallbackRequest(Request request) {
        if (request == null) {
            return null;
        }
        String fallbacks = request.header("X-Cimoc-Fallback");
        if (fallbacks == null || fallbacks.isEmpty()) {
            return request;
        }
        int comma = fallbacks.indexOf(',');
        String nextUrl = comma < 0 ? fallbacks : fallbacks.substring(0, comma);
        String rest = comma < 0 ? "" : fallbacks.substring(comma + 1);
        Request.Builder builder = request.newBuilder().url(nextUrl);
        if (rest.isEmpty()) {
            builder.removeHeader("X-Cimoc-Fallback");
        } else {
            builder.header("X-Cimoc-Fallback", rest);
        }
        return builder.build();
    }

    public static class ParseErrorException extends Exception {
    }

    public static class NetworkErrorException extends Exception {
    }
}
