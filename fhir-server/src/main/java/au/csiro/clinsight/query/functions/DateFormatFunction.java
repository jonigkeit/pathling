/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

package au.csiro.clinsight.query.functions;

import au.csiro.clinsight.query.parsing.ExpressionParserContext;
import au.csiro.clinsight.query.parsing.ParseResult;
import au.csiro.clinsight.query.parsing.ParseResult.FhirPathType;
import au.csiro.clinsight.query.parsing.ParseResult.FhirType;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author John Grimes
 */
public class DateFormatFunction implements ExpressionFunction {

  private static final Set<String> supportedTypes = new HashSet<String>() {{
    add("instant");
    add("dateTime");
    add("date");
  }};

  @Nonnull
  @Override
  public ParseResult invoke(@Nonnull String expression, @Nullable ParseResult input,
      @Nonnull List<ParseResult> arguments) {
    validateInput(input);
    ParseResult argument = validateArgument(arguments);

    ParseResult result = new ParseResult();
    result.setFhirPath(expression);
    String newSqlExpression =
        "date_format(" + input.getSql() + ", " + argument.getFhirPath() + ")";
    result.setSql(newSqlExpression);
    result.setFhirPathType(FhirPathType.STRING);
    result.setFhirType(FhirType.STRING);
    result.setPrimitive(true);
    result.setSingular(input.isSingular());
    return result;
  }

  private void validateInput(ParseResult input) {
    if (input == null || input.getSql() == null || input.getSql().isEmpty()) {
      throw new InvalidRequestException("Missing input expression for dateFormat function");
    }
    if (input.getFhirPathType() != FhirPathType.DATE_TIME) {
      throw new InvalidRequestException(
          "Input to dateFormat function must be a DateTime: " + input.getFhirPath());
    }
  }

  private ParseResult validateArgument(List<ParseResult> arguments) {
    if (arguments.size() != 1) {
      throw new InvalidRequestException("Must pass format argument to dateFormat function");
    }
    ParseResult argument = arguments.get(0);
    if (argument.getFhirPathType() != FhirPathType.STRING) {
      throw new InvalidRequestException(
          "Argument to dateFormat function must be a String: " + argument.getFhirPath());
    }
    return argument;
  }

  @Override
  public void setContext(@Nonnull ExpressionParserContext context) {
  }

}
