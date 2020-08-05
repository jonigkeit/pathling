/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath.function.memberof;

import static au.csiro.pathling.utilities.Preconditions.check;

import au.csiro.pathling.fhir.SimpleCoding;
import au.csiro.pathling.fhir.TerminologyClient;
import au.csiro.pathling.fhir.TerminologyClientFactory;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Row;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumerations.FHIRDefinedType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.slf4j.MDC;

/**
 * Takes a set of Rows with two columns: (1) a correlation identifier, and; (2) a Coding or
 * CodeableConcept to validate. Returns a set of {@link MemberOfResult} objects, which contain the
 * correlation identifier and a boolean result.
 */
@Slf4j
public class MemberOfMapper implements MapPartitionsFunction<Row, MemberOfResult> {

  private static final long serialVersionUID = 2879761794073649202L;

  @Nonnull
  private final String requestId;

  @Nonnull
  private final TerminologyClientFactory terminologyClientFactory;

  @Nonnull
  private final String valueSetUri;

  @Nonnull
  private final FHIRDefinedType fhirType;

  /**
   * @param requestId An identifier used alongside any logging that the mapper outputs
   * @param terminologyClientFactory Used to create instances of the terminology client on workers
   * @param valueSetUri The identifier of the ValueSet that codes will be validated against
   * @param fhirType The type of the input, either Coding or CodeableConcept
   */
  public MemberOfMapper(@Nonnull final String requestId,
      @Nonnull final TerminologyClientFactory terminologyClientFactory,
      @Nonnull final String valueSetUri, @Nonnull final FHIRDefinedType fhirType) {
    this.requestId = requestId;
    this.terminologyClientFactory = terminologyClientFactory;
    this.valueSetUri = valueSetUri;
    this.fhirType = fhirType;
  }

  @Override
  @Nonnull
  public Iterator<MemberOfResult> call(@Nullable final Iterator<Row> inputRows) {
    if (inputRows == null || !inputRows.hasNext()) {
      return Collections.emptyIterator();
    }

    // Add the request ID to the logging context, so that we can track the logging for this
    // request across all workers.
    MDC.put("requestId", requestId);

    // Get all the codings from the input rows, filtering out any incomplete ones.
    final Iterable<Row> inputRowsIterable = () -> inputRows;
    final Function<Row, MemberOfResult> keyMapper = row -> new MemberOfResult(row.getInt(0));
    final Function<Row, List<SimpleCoding>> valueMapper = row ->
        getCodingsFromRow(row.getStruct(1)).stream()
            .filter(SimpleCoding::isNotNull)
            .collect(Collectors.toList());
    final Map<MemberOfResult, List<SimpleCoding>> hashesAndCodes = StreamSupport
        .stream(inputRowsIterable.spliterator(), false)
        .collect(Collectors.toMap(keyMapper, valueMapper));

    // Get the unique set of code system URIs that are present within the codings.
    final List<SimpleCoding> codings = hashesAndCodes.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    final Set<String> codeSystems = codings.stream()
        .map(SimpleCoding::getSystem)
        .collect(Collectors.toSet());

    // Create a ValueSet to represent the intersection of the input codings and the ValueSet
    // described by the URI in the argument.
    final ValueSet intersection = new ValueSet();
    final ValueSetComposeComponent compose = new ValueSetComposeComponent();
    final List<ConceptSetComponent> includes = new ArrayList<>();

    // Create an include section for each unique code system present within the input codings.
    for (final String system : codeSystems) {
      final ConceptSetComponent include = new ConceptSetComponent();
      include.setValueSet(Collections.singletonList(new CanonicalType(valueSetUri)));
      include.setSystem(system);
      @SuppressWarnings("ConstantConditions")
      final List<ConceptReferenceComponent> concepts = codings.stream()
          .filter(coding -> coding.getSystem().equals(system))
          .map(coding -> {
            final ConceptReferenceComponent concept = new ConceptReferenceComponent();
            concept.setCode(coding.getCode());
            return concept;
          })
          .collect(Collectors.toList());
      include.setConcept(concepts);
      includes.add(include);
    }
    compose.setInclude(includes);
    intersection.setCompose(compose);

    // Ask the terminology service to work out the intersection between the set of input codings
    // and the ValueSet identified by the URI in the argument.
    final TerminologyClient terminologyClient = terminologyClientFactory.build(log);
    log.info("Intersecting " + hashesAndCodes.size() + " concepts with " + valueSetUri
        + " using terminology service");
    final ValueSet expansion = terminologyClient
        .expand(intersection, new IntegerType(hashesAndCodes.size()));

    // Build a set of SimpleCodings to represent the codings present in the intersection.
    final Set<SimpleCoding> expandedCodings = expansion.getExpansion().getContains().stream()
        .map(contains -> new SimpleCoding(contains.getSystem(), contains.getCode(),
            contains.getVersion()))
        .collect(Collectors.toSet());

    // Build a MemberOfResult for each of the input rows, with the result indicating whether that
    // coding was present in the intersection.
    final List<MemberOfResult> results = hashesAndCodes.keySet().stream()
        .map(result -> new MemberOfResult(result.getHash(),
            !Collections.disjoint(expandedCodings, hashesAndCodes.get(result))))
        .collect(Collectors.toList());

    return results.iterator();
  }

  @Nonnull
  private List<SimpleCoding> getCodingsFromRow(@Nullable final Row inputCoding) {
    if (inputCoding == null) {
      return Collections.emptyList();
    }
    check(fhirType == FHIRDefinedType.CODING || fhirType == FHIRDefinedType.CODEABLECONCEPT);

    if (fhirType == FHIRDefinedType.CODING) {
      return Collections.singletonList(getCodingFromRow(inputCoding));
    } else {
      return inputCoding.getList(inputCoding.fieldIndex("coding")).stream()
          .map(codingRow -> getCodingFromRow((Row) codingRow))
          .collect(Collectors.toList());
    }
  }

  @Nonnull
  private static SimpleCoding getCodingFromRow(@Nonnull final Row inputCoding) {
    final String system = inputCoding.getString(inputCoding.fieldIndex("system"));
    final String code = inputCoding.getString(inputCoding.fieldIndex("code"));
    final String version = inputCoding.getString(inputCoding.fieldIndex("version"));
    return new SimpleCoding(system, code, version);
  }

}