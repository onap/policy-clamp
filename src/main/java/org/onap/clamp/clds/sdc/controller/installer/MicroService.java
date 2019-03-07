/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
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
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */
package org.onap.clamp.clds.sdc.controller.installer;

import java.util.Objects;

public class MicroService {
  private final String name;
  private final String inputFrom;

  public MicroService(String name, String inputFrom) {
    this.name = name;
    this.inputFrom = inputFrom;
  }
  public String getName() {
    return name;
  }

  public String getInputFrom() {
    return inputFrom;
  }

  @Override
  public String toString() {
    return "MicroService{" +
        "name='" + name + '\'' +
        ", inputFrom='" + inputFrom + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MicroService that = (MicroService) o;
    return name.equals(that.name) &&
        inputFrom.equals(that.inputFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, inputFrom);
  }
}
