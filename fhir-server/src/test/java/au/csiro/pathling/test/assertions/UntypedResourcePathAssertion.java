/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import au.csiro.pathling.fhirpath.UntypedResourcePath;
import java.util.Arrays;
import java.util.HashSet;
import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;

/**
 * @author John Grimes
 */
public class UntypedResourcePathAssertion extends FhirPathAssertion<UntypedResourcePathAssertion> {

  @Nonnull
  private final UntypedResourcePath fhirPath;

  UntypedResourcePathAssertion(@Nonnull final UntypedResourcePath fhirPath) {
    super(fhirPath);
    this.fhirPath = fhirPath;
  }

  @Nonnull
  public DatasetAssert selectUntypedResourceResult() {
    return new DatasetAssert(fhirPath.getDataset()
        .select(fhirPath.getIdColumn(), fhirPath.getTypeColumn(), fhirPath.getValueColumn())
        .orderBy(fhirPath.getIdColumn(), fhirPath.getTypeColumn(), fhirPath.getValueColumn()));
  }

  @Nonnull
  public UntypedResourcePathAssertion hasPossibleTypes(@Nonnull final ResourceType... types) {
    final HashSet<Object> typeSet = new HashSet<>(Arrays.asList(types));
    assertEquals(typeSet, fhirPath.getPossibleTypes());
    return this;
  }

}