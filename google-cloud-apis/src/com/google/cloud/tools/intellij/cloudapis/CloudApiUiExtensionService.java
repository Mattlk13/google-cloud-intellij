/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.cloudapis;

import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.project.Project;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JLabel;

public interface CloudApiUiExtensionService {
  enum EXTENSION_COMPONENT_LOCATION {
    TOP_LINE_1,
    TOP_LINE_2,
    BOTTOM_LINE_1,
    BOTTOM_LINE_2
  }

  EnumMap<EXTENSION_COMPONENT_LOCATION, JComponent> createAdditionalCloudApiComponents();

  void setCloudApiUiPresenter(CloudApiUiPresenter cloudApiUiPresenter);

  List<Optional<String>> onCloudLibrarySelectionChange(CloudLibrary currentCloudLibrary);

  interface CloudApiUiPresenter {
    Project getProject();

    ModulesComboBox getModulesComboBox();

    JLabel getVersionLabel();
  }
}
