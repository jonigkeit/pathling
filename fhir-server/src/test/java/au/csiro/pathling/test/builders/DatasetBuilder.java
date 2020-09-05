/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.builders;

import static au.csiro.pathling.test.helpers.SparkHelpers.getSparkSession;
import static au.csiro.pathling.utilities.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;

/**
 * Assists with the task of creating Datasets and lists of Rows for testing purposes.
 *
 * @author John Grimes
 */
public class DatasetBuilder {

  @Nonnull
  private final SparkSession spark;

  @Nonnull
  private final Metadata metadata;

  @Nonnull
  private final List<StructField> datasetColumns = new ArrayList<>();

  @Nonnull
  private final List<Row> datasetRows = new ArrayList<>();

  @Nonnull
  private final List<StructField> structColumns = new ArrayList<>();

  public DatasetBuilder() {
    spark = getSparkSession();
    final Metadata metadata = new MetadataBuilder().build();
    checkNotNull(metadata);
    this.metadata = metadata;
  }

  @Nonnull
  public DatasetBuilder withIdColumn() {
    final StructField column = new StructField("id", DataTypes.StringType, true, metadata);
    datasetColumns.add(column);
    return this;
  }

  @Nonnull
  public DatasetBuilder withValueColumn(@Nonnull final DataType dataType) {
    final StructField column = new StructField("value", dataType, true, metadata);
    datasetColumns.add(column);
    return this;
  }

  @Nonnull
  public DatasetBuilder withTypeColumn() {
    final StructField column = new StructField("type", DataTypes.StringType, true, metadata);
    datasetColumns.add(column);
    return this;
  }

  @Nonnull
  public DatasetBuilder withColumn(@Nonnull final String name, @Nonnull final DataType dataType) {
    final StructField column = new StructField(name, dataType, true, metadata);
    datasetColumns.add(column);
    return this;
  }

  @Nonnull
  public DatasetBuilder withRow(@Nonnull final Object... values) {
    final Row row = RowFactory.create(values);
    datasetRows.add(row);
    return this;
  }

  @Nonnull
  public DatasetBuilder withRow(@Nonnull final Row row) {
    datasetRows.add(row);
    return this;
  }

  @Nonnull
  public DatasetBuilder withIdValueRows(@Nonnull final Iterable<String> ids,
      @Nonnull final Function<String, Object> valueProducer) {
    ids.forEach(id -> {
      this.withRow(id, valueProducer.apply(id));
    });
    return this;
  }

  @Nonnull
  public DatasetBuilder changeValue(@Nonnull final String id, @Nonnull final Object value) {
    for (int i = 0; i < datasetRows.size(); i++) {
      final Row row = datasetRows.get(i);
      checkNotNull(row);

      final String string = row.getString(0);
      checkNotNull(string);

      if (string.equals(id)) {
        datasetRows.set(i, RowFactory.create(id, value));
      }
    }
    return this;
  }

  @Nonnull
  public DatasetBuilder changeValues(@Nonnull final Object value,
      @Nonnull final Iterable<String> ids) {
    ids.forEach(id -> changeValue(id, value));
    return this;
  }

  @Nonnull
  public DatasetBuilder withIdsAndValue(@Nonnull final Object value,
      @Nonnull final Iterable<String> ids) {
    for (final String id : ids) {
      final Row row = RowFactory.create(id, value);
      datasetRows.add(row);
    }
    return this;
  }

  @Nonnull
  public DatasetBuilder withStructColumn(@Nonnull final String name,
      @Nonnull final DataType dataType) {
    final StructField column = new StructField(name, dataType, true, metadata);
    structColumns.add(column);
    return this;
  }

  @Nonnull
  public DatasetBuilder withStructTypeColumns(@Nonnull final StructType structType) {
    final StructField[] fields = structType.fields();
    checkNotNull(fields);

    structColumns.addAll(Arrays.asList(fields));
    return this;
  }

  @Nonnull
  public Dataset<Row> build() {
    if (!structColumns.isEmpty()) {
      throw new RuntimeException(
          "Called build() on a DatasetBuilder with struct columns, did you mean to use "
              + "buildWithStructValue()?");
    } else if (datasetColumns.isEmpty()) {
      throw new RuntimeException("Called build() on a DatasetBuilder with no columns");
    }
    return getDataset(datasetColumns);
  }

  @Nonnull
  public Dataset<Row> buildWithStructValue() {
    final List<StructField> columns = new ArrayList<>(datasetColumns);
    columns.add(new StructField(
        "value",
        DataTypes.createStructType(structColumns),
        true,
        metadata));
    return getDataset(columns);
  }

  @Nonnull
  public List<Row> getRows() {
    return datasetRows;
  }

  @Nonnull
  private Dataset<Row> getDataset(@Nonnull final List<StructField> columns) {
    final StructType schema;
    schema = new StructType(columns.toArray(new StructField[]{}));

    final Dataset<Row> dataFrame = spark.createDataFrame(datasetRows, schema);
    checkNotNull(dataFrame);

    return dataFrame;
  }

  @Nonnull
  public StructType getStructType() {
    return new StructType(datasetColumns.toArray(new StructField[]{}));
  }

}