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

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.common.collect.ImmutableSet;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class JavaCloudApiUiExtensionProvider implements CloudApiUiExtensionProvider {
  private JLabel bomSelectorLabel;
  private BomComboBox bomComboBox;

  private ModulesComboBox moduleComboBox;

  @Override
  public Set<JComponent> createAdditionalCloudApiComponents() {
    bomComboBox = new BomComboBox();
    bomSelectorLabel = new JBLabel(JavaCloudApisMessageBundle.message("cloud.libraries.bom.selector.label"));

    return ImmutableSet.of(bomSelectorLabel, bomComboBox);
  }

  @Override
  public void setCloudApiComponents(Project project, ModulesComboBox moduleComboBox, JLabel versionLabel) {
    this.moduleComboBox = moduleComboBox;

    if (ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.BOM)) {
      bomComboBox.init(project, moduleComboBox, versionLabel);
    } else {
      hideBomUI();
    }
  }

  private void hideBomUI() {
    bomSelectorLabel.setVisible(false);
    bomSelectorLabel.setVisible(false);
  }
}
