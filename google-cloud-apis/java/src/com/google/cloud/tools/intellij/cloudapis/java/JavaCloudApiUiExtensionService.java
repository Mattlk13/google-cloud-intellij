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

package com.google.cloud.tools.intellij.cloudapis.java;

import static com.google.cloud.tools.intellij.cloudapis.GoogleCloudApiDetailsPanel.makeLink;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.cloudapis.CloudApiUiExtensionService;
import com.google.cloud.tools.intellij.cloudapis.GoogleCloudApisMessageBundle;
import com.google.cloud.tools.intellij.cloudapis.java.CloudApiMavenService.LibraryVersionFromBomException;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.components.JBLabel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;

public class JavaCloudApiUiExtensionService implements CloudApiUiExtensionService {
  private JLabel bomSelectorLabel;
  private BomComboBox bomComboBox;
  private CloudApiUiPresenter cloudApiUiPresenter;

  @Override
  public EnumMap<EXTENSION_COMPONENT_LOCATION, JComponent> createAdditionalCloudApiComponents() {
    bomComboBox = new BomComboBox();
    bomComboBox.init(cloudApiUiPresenter.getProject(), cloudApiUiPresenter.getModulesComboBox());
    bomSelectorLabel =
        new JBLabel(JavaCloudApisMessageBundle.message("cloud.libraries.bom.selector.label"));

    bomComboBox.addActionListener(
        event ->
            cloudApiUiPresenter
                .getSelectedLibrary()
                .ifPresent(
                    cloudLibrary -> {
                      if (bomComboBox.getSelectedItem() != null) {
                        updateManagedLibraryVersionFromBom(
                            cloudLibrary, bomComboBox.getSelectedItem().toString());
                      }
                    }));

    EnumMap<EXTENSION_COMPONENT_LOCATION, JComponent> components =
        new EnumMap<>(EXTENSION_COMPONENT_LOCATION.class);
    components.put(EXTENSION_COMPONENT_LOCATION.BOTTOM_LINE_1, bomSelectorLabel);
    components.put(EXTENSION_COMPONENT_LOCATION.BOTTOM_LINE_2, bomComboBox);

    return components;
  }

  @Override
  public void setCloudApiUiPresenter(CloudApiUiPresenter cloudApiUiPresenter) {
    this.cloudApiUiPresenter = cloudApiUiPresenter;
  }

  @Override
  public List<Optional<String>> onCloudLibrarySelectionChange(CloudLibrary currentCloudLibrary) {
    final List<Optional<String>> links = Lists.newArrayList();

    if (currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
                String currentBomVersion =
                    bomComboBox.getSelectedItem() != null
                        ? bomComboBox.getSelectedItem().toString()
                        : null;
                if (currentBomVersion != null) {
                  updateManagedLibraryVersionFromBom(currentCloudLibrary, currentBomVersion);
                } else {
                  if (client.getMavenCoordinates() != null) {
                    cloudApiUiPresenter
                        .getVersionLabel()
                        .setText(
                            GoogleCloudApisMessageBundle.message(
                                "cloud.libraries.version.label",
                                client.getMavenCoordinates().getVersion()));
                  }
                }

                Optional<String> sourceLink =
                    makeLink("cloud.libraries.source.link", client.getSource());
                Optional<String> apiReferenceLink =
                    makeLink("cloud.libraries.apireference.link", client.getApiReference());

                links.add(sourceLink);
                links.add(apiReferenceLink);
              });
    }

    return Collections.unmodifiableList(links);
  }

  @Override
  public void doUserConfirmedCloudLibraryAction(@NotNull Set<CloudLibrary> libraries) {
    CloudLibraryDependencyWriter.addLibraries(
        libraries,
        cloudApiUiPresenter.getModulesComboBox().getSelectedModule(),
        bomComboBox.getSelectedItem() != null ? bomComboBox.getSelectedItem().toString() : null);
  }

  /**
   * Asynchronously fetches and displays the version of the client library that is managed by the
   * given BOM version.
   *
   * @param bomVersion the version of the BOM from which to load the version of the current client
   *     library
   */
  // TODO (eshaul) this unoptimized implementation fetches all managed BOM versions each time the
  // BOM is updated and library is selected. The bomVersion -> managedLibraryVersions results can be
  // cached on disk to reduce network calls.
  @SuppressWarnings("FutureReturnValueIgnored")
  void updateManagedLibraryVersionFromBom(
      CloudLibrary currentCloudLibrary, @NotNull String bomVersion) {
    if (currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
                JLabel versionLabel = cloudApiUiPresenter.getVersionLabel();
                CloudLibraryClientMavenCoordinates coordinates = client.getMavenCoordinates();

                if (coordinates != null) {
                  versionLabel.setIcon(GoogleCloudCoreIcons.LOADING);
                  versionLabel.setText("");

                  ThreadUtil.getInstance()
                      .executeInBackground(
                          () -> {
                            try {
                              Optional<String> versionOptional =
                                  CloudApiMavenService.getInstance()
                                      .getManagedDependencyVersion(coordinates, bomVersion);

                              if (versionOptional.isPresent()) {
                                ApplicationManager.getApplication()
                                    .invokeAndWait(
                                        () -> {
                                          versionLabel.setText(
                                              GoogleCloudApisMessageBundle.message(
                                                  "cloud.libraries.version.label",
                                                  versionOptional.get()));
                                        },
                                        ModalityState.any());

                                versionLabel.setIcon(null);
                              } else {
                                versionLabel.setText(
                                    GoogleCloudApisMessageBundle.message(
                                        "cloud.libraries.version.label",
                                        GoogleCloudApisMessageBundle.message(
                                            "cloud.libraries.version.notfound.text", bomVersion)));
                                versionLabel.setIcon(General.Error);
                              }
                            } catch (LibraryVersionFromBomException ex) {
                              versionLabel.setText(
                                  GoogleCloudApisMessageBundle.message(
                                      "cloud.libraries.version.label",
                                      GoogleCloudApisMessageBundle.message(
                                          "cloud.libraries.version.exception.text")));
                              versionLabel.setIcon(General.Error);
                            }
                          });
                }
              });
    }
  }

  private void hideBomUI() {
    bomSelectorLabel.setVisible(false);
    bomSelectorLabel.setVisible(false);
  }
}
