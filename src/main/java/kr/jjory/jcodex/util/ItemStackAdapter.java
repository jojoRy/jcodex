package kr.jjory.jcodex.util;

import com.google.gson.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

/**
 * Gson이 ItemStack을 JSON으로 직렬화/역직렬화할 수 있도록 돕는 어댑터 클래스입니다.
 */
public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(src);
            dataOutput.close();
            return new JsonPrimitive(Base64Coder.encodeLines(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new JsonParseException("ItemStack 직렬화 중 오류 발생", e);
        }
    }

    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonNull()) {
            return null;
        }
        try {
            byte[] data = Base64Coder.decodeLines(json.getAsString());
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack itemStack = (ItemStack) dataInput.readObject();
            dataInput.close();
            return itemStack;
        } catch (Exception e) {
            throw new JsonParseException("ItemStack 역직렬화 중 오류 발생", e);
        }
    }
}
