package com.bdl.auto.adapter;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

/**
 * Tests for the {@linkplain DefaultValuesAdapterWriter} class.
 *
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class ThrowingAdapterWriterTest {

  @Test
  public void testSimpleClass() throws Exception {
    ClassMetadata type = ClassMetadata.builder()
        .setCategory(ClassMetadata.Category.CLASS)
        .setType(TypeMetadata.builder()
            .setPackageName("com.bdl.auto.adapter")
            .setName("Simple")
            .build())
        .addMethod(
            MethodMetadata.builder()
                .setVisibility(Visibility.PUBLIC)
                .setIsAbstract(true)
                .setType(TestingTypes.INT)
                .setName("add")
                .addParameter(ParameterMetadata.of(TestingTypes.INT, "first"))
                .addParameter(ParameterMetadata.of(TestingTypes.INT, "second"))
                .build())
        .addMethod(
            MethodMetadata.builder()
                .setVisibility(Visibility.PUBLIC)
                .setIsAbstract(true)
                .setType(TestingTypes.STRING)
                .setName("repeat")
                .addParameter(ParameterMetadata.of(TestingTypes.STRING, "template"))
                .addParameter(ParameterMetadata.of(TestingTypes.INT, "times"))
                .build())
        .addMethod(
            MethodMetadata.builder()
                .setVisibility(Visibility.PUBLIC)
                .setIsAbstract(true)
                .setType(TestingTypes.THING)
                .setName("getThing")
                .build())
        .build();

    assertOutput(type);
  }

  @Test
  public void testSimpleInterface() throws Exception {
    ClassMetadata type = ClassMetadata.builder()
        .setCategory(ClassMetadata.Category.INTERFACE)
        .setType(TypeMetadata.builder()
            .setPackageName("com.bdl.auto.adapter")
            .setName("SimpleInterface")
            .build())
        .addMethod(
            MethodMetadata.builder()
                .setVisibility(Visibility.PUBLIC)
                .setIsAbstract(true)
                .setType(TestingTypes.INT)
                .setName("add")
                .addParameter(ParameterMetadata.of(TestingTypes.INT, "first"))
                .addParameter(ParameterMetadata.of(TestingTypes.INT, "second"))
                .build())
        .build();

    assertOutput(type);
  }

  @Test
  public void testParameterized() throws Exception {
    ClassMetadata type = ClassMetadata.builder()
        .setCategory(ClassMetadata.Category.INTERFACE)
        .setType(TypeMetadata.builder()
            .setPackageName("com.bdl.auto.adapter")
            .setName("Parameterized")
            .addParam(TestingTypes.PARAM_T_EXTENDS_FOO)
            .build())
        .addMethod(
            MethodMetadata.builder()
                .setVisibility(Visibility.PUBLIC)
                .setIsAbstract(true)
                .setType(TestingTypes.PARAM_T)
                .setName("frozzle")
                .addParameter(ParameterMetadata.of(TestingTypes.PARAM_T, "input"))
                .build())
        .build();

    assertOutput(type);
  }

  @Test
  public void testHasAnImplementedMethod() throws Exception {
    MethodMetadata addMethod = MethodMetadata.builder()
        .setVisibility(Visibility.PUBLIC)
        .setType(TestingTypes.INT)
        .setName("add")
        .addParameter(ParameterMetadata.of(TestingTypes.INT, "first"))
        .addParameter(ParameterMetadata.of(TestingTypes.INT, "second"))
        .build();
    MethodMetadata subtractMethod = MethodMetadata.builder()
        .setVisibility(Visibility.PUBLIC)
        .setIsAbstract(true)
        .setType(TestingTypes.INT)
        .setName("subtract")
        .addParameter(ParameterMetadata.of(TestingTypes.INT, "first"))
        .addParameter(ParameterMetadata.of(TestingTypes.INT, "second"))
        .build();
    ClassMetadata type = ClassMetadata.builder()
        .setCategory(ClassMetadata.Category.CLASS)
        .setType(TypeMetadata.builder()
            .setPackageName("com.bdl.auto.adapter")
            .setName("Partial")
            .build())
        .addMethod(addMethod)
        .addMethod(subtractMethod)
        .build();

    assertOutput(type);
  }

  @Test
  public void testHasConstructors() throws Exception {
    ClassMetadata type = ClassMetadata.builder()
        .setCategory(ClassMetadata.Category.CLASS)
        .setType(TypeMetadata.builder()
            .setPackageName("com.bdl.auto.adapter")
            .setName("Constructable")
            .build())
        .addConstructor(ConstructorMetadata.builder()
            .visibility(Visibility.PUBLIC)
            .addParameter(ParameterMetadata.of(TestingTypes.INT, "arg1"))
            .addParameter(ParameterMetadata.of(TestingTypes.STRING, "arg2"))
            .build())
        .addConstructor(ConstructorMetadata.builder()
            .visibility(Visibility.PACKAGE_LOCAL)
            .addParameter(ParameterMetadata.of(TestingTypes.STRING, "arg1"))
            .build())
        .addMethod(MethodMetadata.builder()
            .setVisibility(Visibility.PUBLIC)
            .setIsAbstract(true)
            .setType(TestingTypes.INT)
            .setName("add")
            .addParameter(ParameterMetadata.of(TestingTypes.INT, "first"))
            .addParameter(ParameterMetadata.of(TestingTypes.INT, "second"))
            .build())
        .addMethod(MethodMetadata.builder()
            .setVisibility(Visibility.PUBLIC)
            .setIsAbstract(true)
            .setType(TestingTypes.INT)
            .setName("subtract")
            .addParameter(ParameterMetadata.of(TestingTypes.INT, "first"))
            .addParameter(ParameterMetadata.of(TestingTypes.INT, "second"))
            .build())
        .build();

    assertOutput(type);
  }

  private void assertOutput(ClassMetadata type) throws Exception {
    final Map<String, Writer> writerMap = Maps.newHashMap();

    ThrowingAdapterWriter writer = new ThrowingAdapterWriter(
        new Function<String, Writer>() {
          @Override
          public Writer apply(String input) {
            StringWriter writer = new StringWriter();
            writerMap.put(input + ".txt", writer);
            return writer;
          }
        });

    String key = String.format("%s.%s.txt", type.type().packageName(), type.decoratedName("Throwing"));
    writer.write(type);

    URL resource = getClass().getClassLoader().getResource(key);
    String file = Resources.toString(resource, Charsets.UTF_8);

    assertThat(writerMap.get(key).toString()).isEqualTo(file);
  }
}
