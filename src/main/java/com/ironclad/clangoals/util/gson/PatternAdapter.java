package com.ironclad.clangoals.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

public class PatternAdapter implements JsonSerializer<Pattern>, JsonDeserializer<Pattern>
{
	@Override
	public JsonElement serialize(Pattern src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("pattern", src.pattern());
		jsonObject.addProperty("flags", src.flags());
		return jsonObject;
	}

	@SuppressWarnings("MagicConstant")
	@Override
	public Pattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		String pattern = jsonObject.get("pattern").getAsString();
		int flags = jsonObject.get("flags").getAsInt();
		return Pattern.compile(pattern, flags);
	}
}
