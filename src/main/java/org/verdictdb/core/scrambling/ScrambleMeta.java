/*
 *    Copyright 2018 University of Michigan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.verdictdb.core.scrambling;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.verdictdb.exception.VerdictDBValueException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Table-specific information
 *
 * @author Yongjoo Park
 */
@JsonPropertyOrder({
  "schemaName",
  "tableName",
  "originalSchemaName",
  "originalTableName",
  "aggregationBlockColumn",
  "aggregationBlockCount",
  "tierColumn",
  "numberOfTiers",
  "method"
})
public class ScrambleMeta implements Serializable {

  private static final long serialVersionUID = -8422601151874567149L;

  // key
  String schemaName;

  String tableName;

  // aggregation block
  String aggregationBlockColumn; // agg block number (0 to count-1)

  int aggregationBlockCount; // agg block total count

  // tier
  String tierColumn;

  int numberOfTiers;

  // reference to the original tables
  String originalSchemaName;

  String originalTableName;

  // scramble method used
  String method;

  /**
   * The probability mass function of the sizes of the aggregation blocks for a tier. The key is the
   * id of a tier (e.g., 0, 1, ..., 3), and the list is the cumulative distribution. The length of
   * the cumulative distribution must be equal to aggregationBlockCount.
   */
  @JsonProperty("cumulativeDistributions")
  Map<Integer, List<Double>> cumulativeDistributionForTier = new HashMap<>();

  // subsample column; not used currently
  @JsonIgnore String subsampleColumn;

  public ScrambleMeta() {}

  public ScrambleMeta(
      String scrambleSchemaName,
      String scrambleTableName,
      String originalSchemaName,
      String originalTableName,
      String blockColumn,
      int blockCount,
      String tierColumn,
      int tierCount,
      Map<Integer, List<Double>> cumulativeMassDistributionPerTier)
      throws VerdictDBValueException {

    if (tierCount != cumulativeMassDistributionPerTier.size()) {
      throw new VerdictDBValueException("The number of tiers don't match.");
    }
    for (Entry<Integer, List<Double>> p : cumulativeMassDistributionPerTier.entrySet()) {
      List<Double> dist = p.getValue();
      if (dist == null) {
        throw new VerdictDBValueException("NULL is passed for a cumulative distribution.");
      }
      if (blockCount != dist.size()) {
        throw new VerdictDBValueException("The number of blocks don't match.");
      }
    }

    this.schemaName = scrambleSchemaName;
    this.tableName = scrambleTableName;
    this.aggregationBlockColumn = blockColumn;
    this.aggregationBlockCount = blockCount;
    this.tierColumn = tierColumn;
    this.numberOfTiers = tierCount;
    this.originalSchemaName = originalSchemaName;
    this.originalTableName = originalTableName;
    this.cumulativeDistributionForTier = cumulativeMassDistributionPerTier;
  }

  public ScrambleMeta(
      String scrambleSchemaName,
      String scrambleTableName,
      String originalSchemaName,
      String originalTableName,
      String blockColumn,
      int blockCount,
      String tierColumn,
      int tierCount,
      Map<Integer, List<Double>> cumulativeMassDistributionPerTier,
      String method)
      throws VerdictDBValueException {

    this(
        scrambleSchemaName,
        scrambleTableName,
        originalSchemaName,
        originalTableName,
        blockColumn,
        blockCount,
        tierColumn,
        tierCount,
        cumulativeMassDistributionPerTier);

    this.method = method;
  }

  public String getAggregationBlockColumn() {
    return aggregationBlockColumn;
  }

  public int getAggregationBlockCount() {
    return aggregationBlockCount;
  }

  public List<Double> getCumulativeDistributionForTier(int tier) {
    return cumulativeDistributionForTier.get(tier);
  }

  public int getNumberOfTiers() {
    return numberOfTiers;
  }

  public String getOriginalSchemaName() {
    return originalSchemaName;
  }

  public String getOriginalTableName() {
    return originalTableName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getSubsampleColumn() {
    return subsampleColumn;
  }

  public String getTableName() {
    return tableName;
  }

  public String getTierColumn() {
    return tierColumn;
  }

  public String getMethod() {
    return method;
  }

  public void setAggregationBlockColumn(String aggregationBlockColumn) {
    this.aggregationBlockColumn = aggregationBlockColumn;
  }

  public void setAggregationBlockCount(int aggregationBlockCount) {
    this.aggregationBlockCount = aggregationBlockCount;
  }

  public void setCumulativeDistributionForTier(
      Map<Integer, List<Double>> cumulativeDistributionForTier) {
    this.cumulativeDistributionForTier = cumulativeDistributionForTier;
  }

  public void setNumberOfTiers(int numberOfTiers) {
    this.numberOfTiers = numberOfTiers;
  }

  public void setOriginalSchemaName(String originalSchemaName) {
    this.originalSchemaName = originalSchemaName;
  }

  public void setOriginalTableName(String originalTableName) {
    this.originalTableName = originalTableName;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public void setSubsampleColumn(String subsampleColumn) {
    this.subsampleColumn = subsampleColumn;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void setTierColumn(String tierColumn) {
    this.tierColumn = tierColumn;
  }

  public String toJsonString() {
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonString;
    try {
      jsonString = objectMapper.writeValueAsString(this);
      return jsonString;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ScrambleMeta fromJsonString(String jsonString) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      ScrambleMeta meta = objectMapper.readValue(jsonString, ScrambleMeta.class);
      return meta;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}