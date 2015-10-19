/*
 * Copyright (C) 2014 The Calrissian Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package org.calrissian.flowmix.core.model.op;

import org.calrissian.flowmix.api.Order;
import org.calrissian.flowmix.api.Policy;
import org.calrissian.flowmix.core.model.RequiresPartitioning;
import org.calrissian.mango.domain.Pair;

import java.util.List;

public class SortOp implements FlowOp, RequiresPartitioning {

  public static final String SORT = "sort";

  private List<Pair<String, Order>> sortBy;
  private boolean clearOnTrigger = false;   // this determines whether or or not the dataset is sorted all the time
  private Policy evictionPolicy;
  private long evictionThreshold;
  private Policy triggerPolicy;
  private long triggerThreshold;
  private boolean progressive;

  public SortOp(List<Pair<String,Order>> sortBy, boolean clearOnTrigger, Policy evictionPolicy, long evictionThreshold, Policy triggerPolicy, long triggerThreshold, boolean progressive) {
    this.sortBy = sortBy;
    this.clearOnTrigger = clearOnTrigger;
    this.evictionPolicy = evictionPolicy;
    this.evictionThreshold = evictionThreshold;
    this.triggerPolicy = triggerPolicy;
    this.triggerThreshold = triggerThreshold;
    this.progressive = progressive;
  }


  public boolean isProgressive() {
    return progressive;
  }

  public List<Pair<String,Order>> getSortBy() {
    return sortBy;
  }

  public boolean isClearOnTrigger() {
    return clearOnTrigger;
  }

  public Policy getEvictionPolicy() {
    return evictionPolicy;
  }

  public long getEvictionThreshold() {
    return evictionThreshold;
  }

  public Policy getTriggerPolicy() {
    return triggerPolicy;
  }

  public long getTriggerThreshold() {
    return triggerThreshold;
  }


  @Override
  public boolean equals(Object o) {

    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SortOp sortOp = (SortOp) o;

    if (clearOnTrigger != sortOp.clearOnTrigger) return false;
    if (evictionThreshold != sortOp.evictionThreshold) return false;
    if (triggerThreshold != sortOp.triggerThreshold) return false;
    if (evictionPolicy != sortOp.evictionPolicy) return false;
    if (sortBy != null ? !sortBy.equals(sortOp.sortBy) : sortOp.sortBy != null) return false;
    if (triggerPolicy != sortOp.triggerPolicy) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sortBy != null ? sortBy.hashCode() : 0;
    result = 31 * result + (clearOnTrigger ? 1 : 0);
    result = 31 * result + (evictionPolicy != null ? evictionPolicy.hashCode() : 0);
    result = 31 * result + (int) (evictionThreshold ^ (evictionThreshold >>> 32));
    result = 31 * result + (triggerPolicy != null ? triggerPolicy.hashCode() : 0);
    result = 31 * result + (int) (triggerThreshold ^ (triggerThreshold >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "SortOp{" +
            "sortBy=" + sortBy +
            ", clearOnTrigger=" + clearOnTrigger +
            ", evictionPolicy=" + evictionPolicy +
            ", evictionThreshold=" + evictionThreshold +
            ", triggerPolicy=" + triggerPolicy +
            ", triggerThreshold=" + triggerThreshold +
            '}';
  }

  @Override
  public String getComponentName() {
    return SORT;
  }
}
