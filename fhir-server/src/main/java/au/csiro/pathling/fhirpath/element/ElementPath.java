/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath.element;

import au.csiro.pathling.fhirpath.FhirPath;
import au.csiro.pathling.fhirpath.NonLiteralPath;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.hl7.fhir.r4.model.Enumerations.FHIRDefinedType;

/**
 * Represents any FHIRPath expression which refers to an element within a resource.
 *
 * @author John Grimes
 */
public class ElementPath extends NonLiteralPath {

  /**
   * The FHIR data type of the element being represented by this expression.
   * <p>
   * Note that there can be multiple valid FHIR types for a given FHIRPath type, e.g. {@code uri}
   * and {@code code} both map to the {@code String} FHIRPath type.
   *
   * @see <a href="https://hl7.org/fhir/fhirpath.html#types">Using FHIR types in expressions</a>
   */
  @Getter
  @Nonnull
  private final FHIRDefinedType fhirType;

  @Getter(AccessLevel.PROTECTED)
  @Nonnull
  private Optional<ElementDefinition> definition = Optional.empty();

  protected ElementPath(@Nonnull final String expression, @Nonnull final Dataset<Row> dataset,
      @Nonnull final Column idColumn, @Nonnull final Column valueColumn, final boolean singular,
      @Nonnull final FHIRDefinedType fhirType) {
    super(expression, dataset, idColumn, valueColumn, singular);
    this.fhirType = fhirType;
  }

  /**
   * Builds the appropriate subtype of ElementPath based upon the supplied {@link
   * ElementDefinition}.
   * <p>
   * Use this builder when the path is the child of another path, and will need to be traversable.
   *
   * @param parentPath A parent path that this will be a child of, and will inherit identity and
   * origin from
   * @param expression The FHIRPath representation of this path
   * @param dataset A {@link Dataset} that can be used to evaluate this path against data
   * @param valueColumn A {@link Column} within the dataset containing the values of the nodes
   * @param singular An indicator of whether this path represents a single-valued collection
   * @param definition The HAPI element definition that this path should be based upon
   * @return A new ElementPath
   */
  @Nonnull
  public static ElementPath build(@Nonnull final FhirPath parentPath,
      @Nonnull final String expression,
      @Nonnull final Dataset<Row> dataset, @Nonnull final Column valueColumn,
      final boolean singular, @Nonnull final ElementDefinition definition) {
    final Optional<FHIRDefinedType> optionalFhirType = definition.getFhirType();
    if (optionalFhirType.isPresent()) {
      final FHIRDefinedType fhirType = optionalFhirType.get();
      final ElementPath path = ElementPath
          .build(expression, dataset, parentPath.getIdColumn(), valueColumn, singular, fhirType);
      path.setOriginColumn(parentPath.getOriginColumn());
      path.setOriginType(parentPath.getOriginType());
      path.definition = Optional.of(definition);
      return path;
    } else {
      throw new IllegalArgumentException(
          "Attempted to build an ElementPath with an ElementDefinition with no fhirType");
    }
  }

  /**
   * Builds the appropriate subtype of ElementPath based upon the supplied {@link FHIRDefinedType}.
   * <p>
   * Use this builder when the path is derived, e.g. the result of a function.
   *
   * @param expression The FHIRPath representation of this path
   * @param dataset A {@link Dataset} that can be used to evaluate this path against data
   * @param idColumn A {@link Column} within the dataset containing the identity of the subject
   * resource
   * @param valueColumn A {@link Column} within the dataset containing the values of the nodes
   * @param singular An indicator of whether this path represents a single-valued collection
   * @param fhirType The FHIR type that this path should be based upon
   * @return A new ElementPath
   */
  @Nonnull
  public static ElementPath build(@Nonnull final String expression,
      @Nonnull final Dataset<Row> dataset, @Nonnull final Column idColumn,
      @Nonnull final Column valueColumn, final boolean singular,
      @Nonnull final FHIRDefinedType fhirType) {
    return getInstance(expression, dataset, idColumn, valueColumn, singular, fhirType);
  }

  @Nonnull
  private static ElementPath getInstance(@Nonnull final String expression,
      @Nonnull final Dataset<Row> dataset, @Nonnull final Column idColumn,
      @Nonnull final Column valueColumn, final boolean singular,
      @Nonnull final FHIRDefinedType fhirType) {
    // Look up the class that represents an element with the specified FHIR type.
    final Class<? extends ElementPath> elementPathClass = ElementDefinition
        .elementClassForType(fhirType).orElse(ElementPath.class);
    try {
      // Call its constructor and return.
      final Constructor<? extends ElementPath> constructor = elementPathClass
          .getDeclaredConstructor(String.class, Dataset.class, Column.class, Column.class,
              boolean.class, FHIRDefinedType.class);
      return constructor
          .newInstance(expression, dataset, idColumn, valueColumn, singular, fhirType);
    } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException |
        InvocationTargetException e) {
      throw new RuntimeException("Problem building an ElementPath class", e);
    }
  }

  @Nonnull
  @Override
  public Optional<ElementDefinition> getChildElement(@Nonnull final String name) {
    return definition.flatMap(elementDefinition -> elementDefinition.getChildElement(name));
  }
}