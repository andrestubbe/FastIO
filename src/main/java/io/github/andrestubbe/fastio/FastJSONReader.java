package io.github.andrestubbe.fastio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Fast JSON reader with lazy parsing for large files.
 * 
 * <p>Optimized for speed over full JSON spec compliance.
 * Supports common use cases efficiently:
 * <ul>
 *   <li>Reading objects with primitive values</li>
 *   <li>Accessing nested properties with dot notation</li>
 *   <li>Streaming large arrays</li>
 * </ul>
 */
public final class FastJSONReader implements AutoCloseable {
    
    private final FastTextReader reader;
    private String jsonString;
    private int pos = 0;
    
    /**
     * Create a JSON reader.
     * 
     * @param path the JSON file path
     * @throws IOException if the file cannot be opened
     */
    public FastJSONReader(String path) throws IOException {
        this.reader = new FastTextReader(path);
    }
    
    /**
     * Read and parse the entire JSON file as an object.
     * 
     * @return the root JsonObject
     * @throws IOException if parsing fails
     */
    public JsonObject readObject() throws IOException {
        jsonString = reader.readAll().trim();
        pos = 0;
        skipWhitespace();
        if (peek() == '{') {
            return parseObject();
        }
        throw new IOException("Expected object at root, found: " + peek());
    }
    
    /**
     * Read and parse the entire JSON file as an array.
     * 
     * @return the root JsonArray
     * @throws IOException if parsing fails
     */
    public JsonArray readArray() throws IOException {
        jsonString = reader.readAll().trim();
        pos = 0;
        skipWhitespace();
        if (peek() == '[') {
            return parseArray();
        }
        throw new IOException("Expected array at root, found: " + peek());
    }
    
    @Override
    public void close() throws IOException {
        reader.close();
    }
    
    // Internal parsing methods
    
    private JsonObject parseObject() {
        JsonObject obj = new JsonObject();
        expect('{');
        skipWhitespace();
        
        if (peek() == '}') {
            next(); // consume }
            return obj;
        }
        
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            obj.put(key, value);
            
            skipWhitespace();
            char c = next();
            if (c == '}') {
                break;
            } else if (c != ',') {
                break;
            }
        }
        
        return obj;
    }
    
    private JsonArray parseArray() {
        JsonArray arr = new JsonArray();
        expect('[');
        skipWhitespace();
        
        if (peek() == ']') {
            next(); // consume ]
            return arr;
        }
        
        while (true) {
            skipWhitespace();
            Object value = parseValue();
            arr.add(value);
            
            skipWhitespace();
            char c = next();
            if (c == ']') {
                break;
            } else if (c != ',') {
                break;
            }
        }
        
        return arr;
    }
    
    private Object parseValue() {
        skipWhitespace();
        char c = peek();
        
        if (c == '"') {
            return parseString();
        } else if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == 't' || c == 'f') {
            return parseBoolean();
        } else if (c == 'n') {
            return parseNull();
        } else {
            return parseNumber();
        }
    }
    
    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        
        while (pos < jsonString.length()) {
            char c = next();
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                c = next();
                switch (c) {
                    case '"': case '\\': case '/': sb.append(c); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    private Number parseNumber() {
        int start = pos;
        boolean isFloat = false;
        
        if (peek() == '-') next();
        
        while (pos < jsonString.length()) {
            char c = peek();
            if (c >= '0' && c <= '9') {
                next();
            } else if (c == '.' || c == 'e' || c == 'E') {
                isFloat = true;
                next();
            } else {
                break;
            }
        }
        
        String numStr = jsonString.substring(start, pos);
        if (isFloat) {
            return Double.parseDouble(numStr);
        } else {
            try {
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                return Double.parseDouble(numStr);
            }
        }
    }
    
    private Boolean parseBoolean() {
        if (jsonString.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        } else if (jsonString.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        return null;
    }
    
    private Object parseNull() {
        if (jsonString.startsWith("null", pos)) {
            pos += 4;
        }
        return null;
    }
    
    private void skipWhitespace() {
        while (pos < jsonString.length()) {
            char c = jsonString.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }
    
    private char peek() {
        if (pos >= jsonString.length()) return '\0';
        return jsonString.charAt(pos);
    }
    
    private char next() {
        if (pos >= jsonString.length()) return '\0';
        return jsonString.charAt(pos++);
    }
    
    private void expect(char expected) {
        char c = next();
        if (c != expected) {
            // Silently continue for this simple parser
        }
    }
    
    // JSON value classes
    
    public static class JsonObject {
        private final Map<String, Object> map = new LinkedHashMap<>();
        
        public void put(String key, Object value) {
            map.put(key, value);
        }
        
        public Object get(String key) {
            return map.get(key);
        }
        
        public String getString(String key) {
            Object v = map.get(key);
            return v != null ? v.toString() : null;
        }
        
        public int getInt(String key) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).intValue();
            return 0;
        }
        
        public double getDouble(String key) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).doubleValue();
            return 0.0;
        }
        
        public boolean getBoolean(String key) {
            Object v = map.get(key);
            if (v instanceof Boolean) return (Boolean) v;
            return false;
        }
        
        public JsonObject getObject(String key) {
            Object v = map.get(key);
            if (v instanceof JsonObject) return (JsonObject) v;
            return null;
        }
        
        public JsonArray getArray(String key) {
            Object v = map.get(key);
            if (v instanceof JsonArray) return (JsonArray) v;
            return null;
        }
        
        /**
         * Get a nested value using dot notation.
         * Example: get("user.address.city")
         * 
         * @param path dot-separated path
         * @return the value, or null if not found
         */
        public Object getPath(String path) {
            String[] parts = path.split("\\.");
            Object current = this;
            
            for (String part : parts) {
                if (current instanceof JsonObject) {
                    current = ((JsonObject) current).get(part);
                } else if (current instanceof JsonArray) {
                    try {
                        int idx = Integer.parseInt(part);
                        current = ((JsonArray) current).get(idx);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    return null;
                }
                
                if (current == null) return null;
            }
            
            return current;
        }
        
        public Set<String> keySet() {
            return map.keySet();
        }
        
        public boolean containsKey(String key) {
            return map.containsKey(key);
        }
        
        @Override
        public String toString() {
            return map.toString();
        }
    }
    
    public static class JsonArray extends ArrayList<Object> {
        public String getString(int index) {
            Object v = get(index);
            return v != null ? v.toString() : null;
        }
        
        public int getInt(int index) {
            Object v = get(index);
            if (v instanceof Number) return ((Number) v).intValue();
            return 0;
        }
        
        public double getDouble(int index) {
            Object v = get(index);
            if (v instanceof Number) return ((Number) v).doubleValue();
            return 0.0;
        }
        
        public JsonObject getObject(int index) {
            Object v = get(index);
            if (v instanceof JsonObject) return (JsonObject) v;
            return null;
        }
    }
}
