package cloud.fogbow.fs.core.util;

import com.google.gson.Gson;

public class JsonUtils {
	private Gson gson;
	
	public JsonUtils() {
		gson = new Gson();
	}
	
	public <T> T fromJson(String json, Class<T> classOfT) {
		return gson.fromJson(json, classOfT);
	}
}
