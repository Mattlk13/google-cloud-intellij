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

import com.google.cloud.tools.intellij.cloudapis.CloudApiUiExtensionService.EXTENSION_COMPONENT_LOCATION;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import java.util.EnumMap;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class CloudApiUiExtensionServiceManager
    implements CloudApiUiExtensionService.CloudApiUiPresenter {
  private EnumMap<EXTENSION_COMPONENT_LOCATION, JComponent> extensionComponentMap;

  static CloudApiUiExtensionServiceManager getInstance() {
    return ServiceManager.getService(CloudApiUiExtensionServiceManager.class);
  }

  Optional<CloudApiUiExtensionService> getCloudApiUiExtensionService() {
    return Optional.ofNullable(ServiceManager.getService(CloudApiUiExtensionService.class));
  }

  Optional<JComponent> getComponentAt(EXTENSION_COMPONENT_LOCATION extensionComponentLocation) {
    if (extensionComponentMap == null) {
      extensionComponentMap =
          getCloudApiUiExtensionService()
              .map(CloudApiUiExtensionService::createAdditionalCloudApiComponents)
              .orElse(new EnumMap<>(EXTENSION_COMPONENT_LOCATION.class));
    }

    return Optional.ofNullable(extensionComponentMap.get(extensionComponentLocation));
  }

  @Override
  public Project getProject() {
    return null;
  }

  @Override
  public ModulesComboBox getModulesComboBox() {
    return null;
  }

  @Override
  public JLabel getVersionLabel() {
    return null;
  }
}
