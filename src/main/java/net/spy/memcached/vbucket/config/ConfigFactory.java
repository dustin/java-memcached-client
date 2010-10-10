package net.spy.memcached.vbucket.config;

import java.io.File;

import org.codehaus.jettison.json.JSONObject;

public interface ConfigFactory {

    Config create(File file);

    Config create(String data);

    Config create(JSONObject jsonObject);
}
