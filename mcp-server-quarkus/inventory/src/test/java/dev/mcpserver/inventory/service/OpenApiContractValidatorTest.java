package dev.mcpserver.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mcpserver.inventory.model.ListEventsRequest;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenApiContractValidatorTest {

    @Test
    void defaultConstructorLoadsContractAndValidatesPayloads() {
        OpenApiContractValidator validator = new OpenApiContractValidator();

        assertDoesNotThrow(() -> validator.validate("ListEventsRequest", new ListEventsRequest("Paris")));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("ListEventsRequest", Map.of()));
    }

    @Test
    void validatorRejectsUnknownSchemaName() {
        ObjectMapper objectMapper = new ObjectMapper();
      ObjectSchema schema = new ObjectSchema();
      schema.setType("object");

        OpenApiContractValidator validator = new OpenApiContractValidator(objectMapper, Map.of("Known", schema));

        assertThrows(IllegalStateException.class, () -> validator.validate("Unknown", Map.of("a", "b")));
    }

    @Test
    void validatorUsesProvidedSchemaMap() {
        ObjectMapper objectMapper = new ObjectMapper();
      StringSchema citySchema = new StringSchema();
      citySchema.setMinLength(1);
      ObjectSchema schema = new ObjectSchema();
      schema.setType("object");
      schema.setRequired(java.util.List.of("city"));
      schema.setAdditionalProperties(Boolean.FALSE);
      schema.setProperties(new LinkedHashMap<>());
      schema.addProperties("city", citySchema);

      IntegerSchema quantity = new IntegerSchema();
      quantity.setMinimum(java.math.BigDecimal.ONE);
      quantity.setMaximum(java.math.BigDecimal.TEN);
      schema.addProperties("quantity", quantity);

        OpenApiContractValidator validator = new OpenApiContractValidator(objectMapper, Map.of("ListEventsRequest", schema));

        assertDoesNotThrow(() -> validator.validate("ListEventsRequest", Map.of("city", "Rome")));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("ListEventsRequest", Map.of("city", "")));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("ListEventsRequest", Map.of("city", "Rome", "extra", "x")));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("ListEventsRequest", Map.of("city", "Rome", "quantity", 11)));
    }

    @Test
    void validatorCoversTypeValidationBranches() {
      ObjectMapper objectMapper = new ObjectMapper();

      StringSchema citySchema = new StringSchema();
      citySchema.setMinLength(2);
      citySchema.setMaxLength(4);

      IntegerSchema quantitySchema = new IntegerSchema();
      quantitySchema.setMinimum(java.math.BigDecimal.ONE);
      quantitySchema.setMaximum(java.math.BigDecimal.valueOf(2));

      ObjectSchema objectSchema = new ObjectSchema();
      objectSchema.setType("object");
      objectSchema.setRequired(java.util.List.of("city", "quantity"));
      objectSchema.setAdditionalProperties(Boolean.FALSE);
      objectSchema.setProperties(new LinkedHashMap<>());
      objectSchema.addProperties("city", citySchema);
      objectSchema.addProperties("quantity", quantitySchema);

      OpenApiContractValidator validator = new OpenApiContractValidator(objectMapper, Map.of("Contract", objectSchema));

      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", "not-an-object"));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("quantity", 1)));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("city", 9, "quantity", 1)));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("city", "A", "quantity", 1)));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("city", "ABCDE", "quantity", 1)));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("city", "AB", "quantity", "1")));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("city", "AB", "quantity", 0)));
      assertThrows(IllegalArgumentException.class, () -> validator.validate("Contract", Map.of("city", "AB", "quantity", 3)));
      assertDoesNotThrow(() -> validator.validate("Contract", Map.of("city", "AB", "quantity", 2)));
    }

    @Test
    void validatorIgnoresUnknownPropertyTypeSchema() {
      ObjectMapper objectMapper = new ObjectMapper();

      Schema<?> boolSchema = new Schema<>().type("boolean");
      ObjectSchema objectSchema = new ObjectSchema();
      objectSchema.setType("object");
      objectSchema.setProperties(new LinkedHashMap<>());
      objectSchema.addProperties("flag", boolSchema);

      OpenApiContractValidator validator = new OpenApiContractValidator(objectMapper, Map.of("BooleanContract", objectSchema));

      assertDoesNotThrow(() -> validator.validate("BooleanContract", Map.of("flag", true)));
    }

  @Test
  void validatorSupportsSchemaWithoutPropertiesAndRejectsNonObjectSchema() {
    ObjectMapper objectMapper = new ObjectMapper();

    ObjectSchema noPropertyObject = new ObjectSchema();
    noPropertyObject.setType("object");

    Schema<?> nonObjectSchema = new Schema<>().type("string");

    OpenApiContractValidator validator = new OpenApiContractValidator(
        objectMapper,
        Map.of(
            "ObjectContract", noPropertyObject,
            "StringContract", nonObjectSchema
        )
    );

    assertDoesNotThrow(() -> validator.validate("ObjectContract", Map.of()));
    assertDoesNotThrow(() -> validator.validate("StringContract", Map.of("x", "y")));
  }

  @Test
  void validatorHandlesStringAndIntegerWithoutOptionalBounds() {
    ObjectMapper objectMapper = new ObjectMapper();

    StringSchema label = new StringSchema();
    IntegerSchema count = new IntegerSchema();

    ObjectSchema schema = new ObjectSchema();
    schema.setType("object");
    schema.setProperties(new LinkedHashMap<>());
    schema.addProperties("label", label);
    schema.addProperties("count", count);

    OpenApiContractValidator validator = new OpenApiContractValidator(objectMapper, Map.of("NoBounds", schema));

    assertDoesNotThrow(() -> validator.validate("NoBounds", Map.of("label", "ok", "count", 1)));
  }

  @Test
  void validatorTreatsObjectSchemaByInstanceOrPropertiesFallback() {
    ObjectMapper objectMapper = new ObjectMapper();

    ObjectSchema instanceOnlyObject = new ObjectSchema();

    Schema<?> propertiesOnlyObject = new Schema<>();
    propertiesOnlyObject.setProperties(new LinkedHashMap<>());
    propertiesOnlyObject.addProperty("name", new StringSchema());

    OpenApiContractValidator validator = new OpenApiContractValidator(
        objectMapper,
        Map.of(
            "InstanceObject", instanceOnlyObject,
            "PropertiesObject", propertiesOnlyObject
        )
    );

    assertDoesNotThrow(() -> validator.validate("InstanceObject", Map.of()));
    assertDoesNotThrow(() -> validator.validate("PropertiesObject", Map.of("name", "value")));
  }

  @Test
  void validatorCoversNullFieldAndNoPropertiesEdgePaths() {
    ObjectMapper objectMapper = new ObjectMapper();

    ObjectSchema withNullableField = new ObjectSchema();
    withNullableField.setType("object");
    withNullableField.setRequired(java.util.List.of("city"));
    withNullableField.setProperties(new LinkedHashMap<>());
    withNullableField.addProperties("city", new StringSchema());
    withNullableField.addProperties("note", new StringSchema());

    java.util.Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("city", null);

    OpenApiContractValidator validator = new OpenApiContractValidator(objectMapper, Map.of("NullableContract", withNullableField));
    assertThrows(IllegalArgumentException.class, () -> validator.validate("NullableContract", payload));

    ObjectSchema noPropertiesButLocked = new ObjectSchema();
    noPropertiesButLocked.setType("object");
    noPropertiesButLocked.setAdditionalProperties(Boolean.FALSE);

    OpenApiContractValidator noPropertiesValidator = new OpenApiContractValidator(
        objectMapper,
        Map.of("NoProperties", noPropertiesButLocked)
    );
    assertDoesNotThrow(() -> noPropertiesValidator.validate("NoProperties", Map.of()));
  }
}
