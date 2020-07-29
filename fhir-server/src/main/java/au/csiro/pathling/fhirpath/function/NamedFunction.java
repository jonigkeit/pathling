/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath.function;

import static au.csiro.pathling.utilities.Preconditions.checkUserInput;

import au.csiro.pathling.fhirpath.FhirPath;
import au.csiro.pathling.fhirpath.function.memberof.MemberOfFunction;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Represents a named function in FHIRPath.
 *
 * @author John Grimes
 */
public interface NamedFunction {

  /**
   * Mapping of function names to instances of those functions.
   */
  Map<String, NamedFunction> NAME_TO_INSTANCE = new ImmutableMap.Builder<String, NamedFunction>()
      .put("count", new CountFunction())
      .put("resolve", new ResolveFunction())
      .put("ofType", new OfTypeFunction())
      .put("reverseResolve", new ReverseResolveFunction())
      .put("memberOf", new MemberOfFunction())
      .build();

  /**
   * Invokes this function with the specified inputs.
   *
   * @param input A NamedFunctionInput object
   * @return A FhirPath object representing the resulting expression
   */
  @Nonnull
  FhirPath invoke(@Nonnull NamedFunctionInput input);

  /**
   * Retrieves an instance of the function with the specified name.
   *
   * @param name The name of the function
   * @return An instance of a NamedFunction
   */
  @Nonnull
  static NamedFunction getInstance(@Nonnull final String name) {
    final NamedFunction function = NAME_TO_INSTANCE.get(name);
    checkUserInput(function != null, "Unsupported function: " + name);
    return function;
  }

  /**
   * Check that no arguments have been passed within the supplied {@link NamedFunctionInput}.
   *
   * @param functionName The name of the function, used for error reporting purposes
   * @param input The {@link NamedFunctionInput} to check for arguments
   */
  static void checkNoArguments(@Nonnull final String functionName,
      @Nonnull final NamedFunctionInput input) {
    checkUserInput(input.getArguments().isEmpty(),
        "Arguments can not be passed to " + functionName + " function: " + input.getInput()
            .getExpression());
  }

  /**
   * @param input A {@link NamedFunctionInput}
   * @param functionName The name of the function
   * @return A FHIRPath expression for use in the output of the function
   */
  @Nonnull
  static String expressionFromInput(@Nonnull final NamedFunctionInput input,
      @Nonnull final String functionName) {
    final String inputExpression = input.getInput().getExpression();
    final String argumentsExpression = input.getArguments().stream().map(FhirPath::getExpression)
        .collect(
            Collectors.joining(", "));
    final String functionExpression = functionName + "(" + argumentsExpression + ")";
    return inputExpression.isEmpty()
           ? functionExpression
           : inputExpression + "." + functionExpression;
  }

}