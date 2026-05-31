package dev.mcpserver.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class OpenApiContractValidator {

    private final ObjectMapper objectMapper;
    private final Map<String, Schema> schemaByName;

    public OpenApiContractValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaByName = loadSchemas();
    }

    OpenApiContractValidator(ObjectMapper objectMapper, Map<String, Schema> schemaByName) {
        this.objectMapper = objectMapper;
        this.schemaByName = schemaByName;
    }

    public void validate(String schemaName, Object payload) {
        Schema schema = schemaByName.get(schemaName);
        if (schema == null) {
            throw new IllegalStateException("Unknown contract schema: " + schemaName);
        }

        JsonNode node = objectMapper.valueToTree(payload);
        List<String> errors = validateObject(schema, node);
        if (!errors.isEmpty()) {
            String details = String.join("; ", errors);
            throw new IllegalArgumentException("OpenAPI contract validation failed: " + details);
        }
    }

    private Map<String, Schema> loadSchemas() {
        URL contract = Objects.requireNonNull(
                getClass().getClassLoader().getResource("openapi/inventory-mcp.yaml"),
                "Missing OpenAPI contract file: openapi/inventory-mcp.yaml"
        );

        SwaggerParseResult parseResult = new OpenAPIParser().readLocation(contract.toString(), null, null);
        OpenAPI openAPI = Objects.requireNonNull(parseResult.getOpenAPI(), "Unable to parse OpenAPI contract");
        Map<String, Schema> schemas = Objects.requireNonNull(openAPI.getComponents().getSchemas(), "Missing OpenAPI schemas");

        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            schemaMap.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(schemaMap);
    }

    private List<String> validateObject(Schema schema, JsonNode node) {
        List<String> errors = new java.util.ArrayList<>();
        if (!node.isObject()) {
            errors.add("payload must be an object");
            return errors;
        }

        if (schema.getRequired() != null) {
            for (Object requiredFieldObj : schema.getRequired()) {
                String requiredField = requiredFieldObj.toString();
                if (!node.has(requiredField) || node.get(requiredField).isNull()) {
                    errors.add("missing required field: " + requiredField);
                }
            }
        }

        Map<String, Schema> properties = schema.getProperties();
        if (Boolean.FALSE.equals(schema.getAdditionalProperties()) && properties != null) {
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!properties.containsKey(field)) {
                    errors.add("additional property is not allowed: " + field);
                }
            }
        }

        if (properties != null) {
            for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
                String propertyName = propertyEntry.getKey();
                if (!node.has(propertyName) || node.get(propertyName).isNull()) {
                    continue;
                }
                validateProperty(propertyName, propertyEntry.getValue(), node.get(propertyName), errors);
            }
        }

        return errors;
    }

    private void validateProperty(String propertyName, Schema propertySchema, JsonNode value, List<String> errors) {
        if ("string".equals(propertySchema.getType())) {
            if (!value.isTextual()) {
                errors.add(propertyName + " must be a string");
                return;
            }
            int length = value.textValue().length();
            if (propertySchema.getMinLength() != null && length < propertySchema.getMinLength()) {
                errors.add(propertyName + " length must be >= " + propertySchema.getMinLength());
            }
            if (propertySchema.getMaxLength() != null && length > propertySchema.getMaxLength()) {
                errors.add(propertyName + " length must be <= " + propertySchema.getMaxLength());
            }
            return;
        }

        if ("integer".equals(propertySchema.getType())) {
            if (!value.isIntegralNumber()) {
                errors.add(propertyName + " must be an integer");
                return;
            }

            long number = value.longValue();
            if (propertySchema.getMinimum() != null && number < propertySchema.getMinimum().longValue()) {
                errors.add(propertyName + " must be >= " + propertySchema.getMinimum().longValue());
            }
            if (propertySchema.getMaximum() != null && number > propertySchema.getMaximum().longValue()) {
                errors.add(propertyName + " must be <= " + propertySchema.getMaximum().longValue());
            }
        }
    }
}
