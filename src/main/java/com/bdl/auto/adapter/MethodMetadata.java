package com.bdl.auto.adapter;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * Holder of metadata for a method ExecutableElement.
 *
 * @author Ben Leitner
 */
@AutoValue
abstract class MethodMetadata implements Comparable<MethodMetadata>, GeneratesImports {

  abstract Visibility visibility();
  abstract boolean isAbstract();
  abstract ImmutableList<TypeMetadata> typeParameters();
  abstract String name();
  abstract TypeMetadata type();
  abstract ImmutableList<ParameterMetadata> parameters();

  MethodMetadata convertTypeParameters(Map<String, String> paramNameMap) {
    paramNameMap = augmentParamNameMap(paramNameMap);
    Builder metadata = MethodMetadata.builder()
        .setVisibility(visibility())
        .setIsAbstract(isAbstract())
        .setName(name())
        .setType(type().convertTypeParams(paramNameMap));
    for (TypeMetadata typeParam : typeParameters()) {
      metadata.addTypeParameter(typeParam.convertTypeParams(paramNameMap));
    }
    for (ParameterMetadata param : parameters()) {
      metadata.addParameter(ParameterMetadata.of(
          param.type().convertTypeParams(paramNameMap),
          param.name()));
    }
    return metadata.build();
  }

  /** Augments the parameter name map to ensure that type parameters specified for this method are not overridden. */
  private Map<String, String> augmentParamNameMap(Map<String, String> paramNameMap) {
    NameIterator names = new NameIterator();
    ImmutableMap.Builder<String, String> augmented = ImmutableMap.<String, String>builder().putAll(paramNameMap);

    for (TypeMetadata typeParam : typeParameters()) {
      String name = nextClearName(paramNameMap, names, typeParam.name());
      augmented.put(typeParam.name(), name);
    }
    return augmented.build();
  }

  private String nextClearName(Map<String, String> paramNameMap, NameIterator names, String name) {
    String candidate = name;
    while (paramNameMap.containsKey(candidate) || paramNameMap.containsValue(candidate)) {
      candidate = names.next();
    }
    return candidate;
  }

  @Override
  public int compareTo(MethodMetadata that) {
    return ComparisonChain.start()
        .compare(visibility().ordinal(), that.visibility().ordinal())
        .compare(name(), that.name())
        .compare(parameters().size(), that.parameters().size())
        .compare(parameters(), that.parameters(), ParameterMetadata.IMMUTABLE_LIST_COMPARATOR)
        .result();
  }

  @Override
  public ImmutableList<String> getImports() {
    Set<String> imports = Sets.newHashSet();
    for (TypeMetadata typeParam : typeParameters()) {
      imports.addAll(typeParam.getImports());
    }
    imports.addAll(type().getImports());
    for (ParameterMetadata param : parameters()) {
      imports.addAll(param.getImports());
    }
    List<String> allImports = Lists.newArrayList(imports);
    Collections.sort(allImports);
    return ImmutableList.copyOf(allImports);
  }

  private String typeParametersPrefix() {
    if (typeParameters().isEmpty()) {
      return "";
    }
    return String.format("<%s> ", Joiner.on(", ").join(Iterables.transform(typeParameters(),
        new Function<TypeMetadata, String>() {
          @Override
          public String apply(TypeMetadata input) {
            return input.nameBuilder()
                .addSimpleName()
                .addBounds()
                .toString();
          }
        })));
  }

  String fullDescription() {
    return String.format("%s%s%s%s %s(%s)",
        visibility().prefix(),
        isAbstract() ? "abstract " : "",
        typeParametersPrefix(),
        type().nameBuilder()
            .addPackagePrefix()
            .addNestingPrefix()
            .addSimpleName()
            .addSimpleParams()
            .toString(),
        name(),
        Joiner.on(", ").join(parameters()));
  }

  abstract Builder toBuilder();

  static Builder builder() {
    return new AutoValue_MethodMetadata.Builder()
        .setIsAbstract(false);
  }

  static MethodMetadata fromMethod(ExecutableElement element) {
    Preconditions.checkArgument(element.getKind() == ElementKind.METHOD,
        "Element %s is not a method.", element);
    Builder metadata = builder()
        .setVisibility(Visibility.forElement(element))
        .setIsAbstract(element.getModifiers().contains(Modifier.ABSTRACT))
        .setType(TypeMetadata.fromType(element.getReturnType()))
        .setName(element.getSimpleName().toString());

    for (TypeParameterElement typeParam : element.getTypeParameters()) {
      metadata.addTypeParameter(TypeMetadata.fromElement(typeParam));
    }

    for (VariableElement parameter : element.getParameters()) {
      metadata.addParameter(
          ParameterMetadata.of(
              TypeMetadata.fromType(parameter.asType()),
              parameter.getSimpleName().toString()));
    }

    return metadata.build();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    abstract Builder setVisibility(Visibility visibility);
    abstract ImmutableList.Builder<TypeMetadata> typeParametersBuilder();
    abstract Builder setIsAbstract(boolean isAbstract);
    abstract Builder setName(String name);
    abstract Builder setType(TypeMetadata Type);
    abstract ImmutableList.Builder<ParameterMetadata> parametersBuilder();

    Builder addTypeParameter(TypeMetadata metadata) {
      Preconditions.checkArgument(metadata.isTypeParameter(),
          "Cannot add %s as a type parameter.", metadata);
      typeParametersBuilder().add(metadata);
      return this;
    }

    Builder addParameter(ParameterMetadata parameter) {
      parametersBuilder().add(parameter);
      return this;
    }

    abstract MethodMetadata build();
  }

  private static class NameIterator implements Iterator<String> {

    private String current;

    @Override
    public boolean hasNext() {
      return true;
    }

    private String nextString(String current) {
      if (current == null || current.length() == 0) {
        return "A";
      }
      char ch = current.charAt(current.length() - 1);
      String substring = current.substring(0, current.length() - 1);
      if (ch < 'Z') {
        return substring + String.valueOf((char) (ch + 1));
      }
      return nextString(substring) + "A";
    }

    @Override
    public String next() {
      current = nextString(current);
      return current;
    }
  }
}
