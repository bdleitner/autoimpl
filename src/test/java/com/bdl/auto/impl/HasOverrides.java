package com.bdl.auto.impl;

/**
 * An interface that uses implementation overrides.
 *
 * @author Ben Leitner
 */
@AutoImpl(value = ImplOption.THROW_EXCEPTION,
    booleanImpl = ImplOption.RETURN_DEFAULT_VALUE,
    numericImpl = ImplOption.RETURN_DEFAULT_VALUE,
    stringImpl = ImplOption.USE_PARENT
)
interface HasOverrides {

  int intMethod();

  double doubleMethod();

  long longMethod();

  boolean booleanMethod();

  @MethodImpl(ImplOption.THROW_EXCEPTION)
  boolean overriddenBooleanMethod();

  String stringMethod();

  @MethodImpl(ImplOption.RETURN_DEFAULT_VALUE)
  String overriddenStringMethod();

  void voidMethod();

  @MethodImpl(ImplOption.RETURN_DEFAULT_VALUE)
  void overriddenVoidMethod();

  Object objectMethod();
}
