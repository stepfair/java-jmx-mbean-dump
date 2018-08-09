package com.stephenfairchild.jmx.mbean.dump;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.management.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MBeanTimerTask extends TimerTask {

    private static final long MINUTE_IN_MILLIS = 60L * 1000L;

    private static final String FAILED_TO_WRITE_OUTPUT_FILE = "Unable to write to output file '%s'.";
    private static final String FAILED_TO_FIND_JAR_LOCATION = "Failed to retrieve jar location. Using 'java.class.path' of '%s'.";
    private static final String UNABLE_TO_FIND_DEFAULT_CONFIG = "Unable to find default config file '%s' so reading from resource.";
    private static final String INVALID_REFRESH_RATE = "Invalid refresh rate '%s'. Using 1 minute as default.";
    private static final String MALFORMED_OBJECT_NAME = "Malformed object name '%s'.";
    private static final String M_BEAN_EXCEPTION_FORMAT = "MBean exception thrown for object name '%s' and attribute name '%s'";
    private static final String ROW = "\"%s\",\"%s\",\"%s\",\"%s\"";
    private static final String HEADER = "Object,Attribute,Value,Error";
    private static final String VALUE_MALFORMED_OBJECT_NAME = "Object Was Malformed";
    private static final String VALUE_OBJECT_NOT_FOUND = "Object Not Found";
    private static final String VALUE_ATTRIBUTE_NOT_FOUND = "Attribute Not Found";
    private static final String VALUE_ERROR = "Error retrieving Attribute";

    private static final String ROOT_PATH = getRootPath();
    private static final Logger LOGGER = getLogger();
    private static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    private static final MBeanServer M_BEAN_SERVER = ManagementFactory.getPlatformMBeanServer();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<HashMap<String, List<String>>> M_BEANS_TYPE_REFERENCE = new TypeReference<HashMap<String, List<String>>>() {

    };

    @Override
    public void run() {
        Map<String, List<String>> attributeMap = getAttributeMap();
        if (attributeMap != null) {
            writeToCSV(attributeMap);
        }
    }

    static long getPeriodInMillis() {
        String value = System.getProperty("refresh.rate");
        if (value != null) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, String.format(INVALID_REFRESH_RATE, value), e);
            }
        }
        return MINUTE_IN_MILLIS;
    }

    private void writeToCSV(Map<String, List<String>> attributeMap) {
        String csvPath = appendSeparator(System.getProperty("csv.path", ROOT_PATH)) + "mbeans." + PID + ".csv";
        try {
            writeFile(csvPath, buildCSV(attributeMap));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_WRITE_OUTPUT_FILE, csvPath), e);
        }
    }

    private static String getRootPath() {
        try {
            String jarPath = new File(MBeanTimerTask.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            return appendSeparator(stripJar(jarPath));
        } catch (URISyntaxException e) {
            String javaClassPath = appendSeparator(stripJar(System.getProperty("java.class.path", "")));
            LOGGER.log(Level.WARNING, String.format(FAILED_TO_FIND_JAR_LOCATION, javaClassPath), e);
            return javaClassPath;
        }
    }

    private static Logger getLogger() {
        Logger logger = Logger.getLogger(MBeanTimerTask.class.getName());
        logger.setLevel(Level.parse(System.getProperty("log.level", "INFO")));
        return logger;
    }

    private static String stripJar(String path) {
        return path.replaceAll("[^/\\\\]*\\.jar$", "");
    }

    private static String appendSeparator(String path) {
        Pattern p = Pattern.compile("([/\\\\]+)$");
        Matcher m = p.matcher(path);
        return m.find() ? m.replaceFirst("\\" + File.separator) : path + File.separator;
    }

    private InputStream getMBeansJsonStream(String jsonPath, String filePath) throws FileNotFoundException {
        try {
            return new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            if (!ROOT_PATH.equals(jsonPath)) {
                throw e;
            }
            LOGGER.info(String.format(UNABLE_TO_FIND_DEFAULT_CONFIG, filePath));
            return MBeanTimerTask.class.getClassLoader().getResourceAsStream("mbeans.json");
        }
    }

    private Map<String, List<String>> getAttributeMap() {
        String jsonPath = appendSeparator(System.getProperty("json.path", ROOT_PATH));
        String filePath = jsonPath + "mbeans.json";
        try {
            InputStream mBeansJsonStream = getMBeansJsonStream(jsonPath, filePath);
            String mBeansJson = new Scanner(mBeansJsonStream, "UTF-8").useDelimiter("\\A").next();
            return OBJECT_MAPPER.readValue(mBeansJson, M_BEANS_TYPE_REFERENCE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Unable to read config file '%s'", filePath), e);
            return null;
        }
    }

    private String buildCSV(Map<String, List<String>> attributeMap) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HEADER).append("\r\n");
        for (String name : attributeMap.keySet()) {
            ObjectName objectName = getObjectName(name);
            for (String attributeName : attributeMap.get(name)) {
                AttributeValue attributeValue = getAttributeValue(objectName, attributeName);
                stringBuilder.append(String.format(ROW, name, attributeName, attributeValue.getValue(), attributeValue.getError())).append("\r\n");
            }
        }
        return stringBuilder.toString();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void writeFile(String filePath, String csvString) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        file.createNewFile();
        writeFile(file, csvString);
    }

    private void writeFile(File file, String csvString) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(file);
            out.println(csvString);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private AttributeValue getAttributeValue(ObjectName objectName, String attributeName) {
        if (objectName == null) {
            return new AttributeValue(VALUE_MALFORMED_OBJECT_NAME, true);
        }
        try {
            return new AttributeValue(M_BEAN_SERVER.getAttribute(objectName, attributeName), false);
        } catch (AttributeNotFoundException e) {
            return new AttributeValue(VALUE_ATTRIBUTE_NOT_FOUND, true);
        } catch (InstanceNotFoundException e) {
            return new AttributeValue(VALUE_OBJECT_NOT_FOUND, true);
        } catch (MBeanException  e) {
            LOGGER.log(Level.SEVERE, String.format(M_BEAN_EXCEPTION_FORMAT, objectName.getCanonicalName(), attributeName), e);
            return new AttributeValue(VALUE_ERROR, true);
        } catch (ReflectionException e) {
            LOGGER.log(Level.SEVERE, String.format(M_BEAN_EXCEPTION_FORMAT, objectName.getCanonicalName(), attributeName), e);
            return new AttributeValue(VALUE_ERROR, true);
        }
    }

    private ObjectName getObjectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            LOGGER.log(Level.WARNING, String.format(MALFORMED_OBJECT_NAME, name), e);
            return null;
        }
    }

    private static class AttributeValue {

        private final Object value;
        private final Object error;

        AttributeValue(final Object value, final boolean isError) {
            final Object nonNullValue = value == null ? "" : value;
            if (isError) {
                this.value = "";
                this.error = nonNullValue;
            } else {
                this.value = nonNullValue;
                this.error = "";
            }
        }

        Object getValue() {
            return value;
        }

        Object getError() {
            return error;
        }
    }
}
