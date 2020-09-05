/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.builders;

import static org.apache.spark.sql.functions.lit;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.csiro.pathling.fhir.TerminologyClient;
import au.csiro.pathling.fhir.TerminologyClientFactory;
import au.csiro.pathling.fhirpath.FhirPath;
import au.csiro.pathling.fhirpath.ThisPath;
import au.csiro.pathling.fhirpath.parser.ParserContext;
import au.csiro.pathling.io.ResourceReader;
import au.csiro.pathling.test.DefaultAnswer;
import au.csiro.pathling.test.helpers.FhirHelpers;
import au.csiro.pathling.test.helpers.SparkHelpers;
import ca.uhn.fhir.context.FhirContext;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.SparkSession;
import org.mockito.Mockito;

/**
 * @author John Grimes
 */
public class ParserContextBuilder {

  @Nonnull
  private FhirPath inputContext;

  @Nullable
  private ThisPath thisContext;

  @Nonnull
  private FhirContext fhirContext;

  @Nonnull
  private SparkSession sparkSession;

  @Nonnull
  private ResourceReader resourceReader;

  @Nullable
  private TerminologyClient terminologyClient;

  @Nullable
  private TerminologyClientFactory terminologyClientFactory;

  public ParserContextBuilder() {
    inputContext = mock(FhirPath.class);
    when(inputContext.getIdColumn()).thenReturn(Optional.of(lit(null)));
    when(inputContext.getDataset()).thenReturn(SparkHelpers.getSparkSession().emptyDataFrame());
    fhirContext = FhirHelpers.getFhirContext();
    sparkSession = SparkHelpers.getSparkSession();
    resourceReader = Mockito.mock(ResourceReader.class, new DefaultAnswer());
  }

  @Nonnull
  public ParserContextBuilder inputContext(@Nonnull final FhirPath inputContext) {
    this.inputContext = inputContext;
    return this;
  }

  @Nonnull
  public ParserContextBuilder inputExpression(@Nonnull final String inputExpression) {
    when(inputContext.getExpression()).thenReturn(inputExpression);
    return this;
  }

  @Nonnull
  public ParserContextBuilder idColumn(@Nonnull final Column idColumn) {
    when(inputContext.getIdColumn()).thenReturn(Optional.of(idColumn));
    return this;
  }

  @Nonnull
  public ParserContextBuilder thisContext(@Nonnull final ThisPath thisContext) {
    this.thisContext = thisContext;
    return this;
  }

  @Nonnull
  public ParserContextBuilder fhirContext(@Nonnull final FhirContext fhirContext) {
    this.fhirContext = fhirContext;
    return this;
  }

  @Nonnull
  public ParserContextBuilder sparkSession(@Nonnull final SparkSession sparkSession) {
    this.sparkSession = sparkSession;
    return this;
  }

  @Nonnull
  public ParserContextBuilder resourceReader(@Nonnull final ResourceReader resourceReader) {
    this.resourceReader = resourceReader;
    return this;
  }

  @Nonnull
  public ParserContextBuilder terminologyClient(
      @Nonnull final TerminologyClient terminologyClient) {
    this.terminologyClient = terminologyClient;
    return this;
  }

  @Nonnull
  public ParserContextBuilder terminologyClientFactory(
      @Nonnull final TerminologyClientFactory terminologyClientFactory) {
    this.terminologyClientFactory = terminologyClientFactory;
    return this;
  }

  @Nonnull
  public ParserContext build() {
    return new ParserContext(inputContext, Optional.ofNullable(thisContext), fhirContext,
        sparkSession, resourceReader, Optional.ofNullable(terminologyClient),
        Optional.ofNullable(terminologyClientFactory));
  }

}