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

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.cloudapis.GoogleCloudApisMessageBundle;
import com.google.cloud.tools.intellij.cloudapis.java.CloudApiMavenService.LibraryVersionFromBomException;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.google.common.collect.Lists;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

class BomComboBox extends JComboBox<String> {
  private static final int NUM_BOM_VERSIONS_TO_SHOW = 5;

  private Project project;
  private ModulesComboBox modulesComboBox;
  private JLabel versionLabel;

  BomComboBox() {
    setRenderer(new BomVersionRenderer());
  }

  void init(Project project, ModulesComboBox modulesComboBox) {
    this.project = project;
    this.modulesComboBox = modulesComboBox;
    this.versionLabel = versionLabel;
    populateBomVersions();
    addActionListener(
        event -> {
          if (cloudLibrariesTable.getSelectedRow() != -1) {
            if (getSelectedItem() != null) {
              updateManagedLibraryVersionFromBom(
                  getSelectedItem().toString());
            }
          }
        });

    modulesComboBox.addActionListener(event -> populateBomVersions());

  }

  /** Sorts the list of BOM version in reverse order */
  private void sortBomList(List<String> boms) {
    boms.sort(Comparator.reverseOrder());
  }

  /**
   * Populates the BOM {@link JComboBox} with the fetched versions. If there is already a
   * preconfigured BOM in the corresponding module's pom.xml, then its version will be preselected.
   * If there are no versions returned and there is no preconfigured BOM, then the BOM UX is hidden.
   *
   * <p>Sorts the displayable versions in reverse order, and limits the number shown to some value
   * N.
   */
  // TODO (eshaul): make async with loader icons
  void populateBomVersions() {
    List<String> availableBomVersions =
        Lists.newArrayList(CloudApiMavenService.getInstance().getAllBomVersions());

    Optional<String> configuredBomVersion =
        CloudLibraryProjectState.getInstance(project)
            .getCloudLibraryBomVersion(modulesComboBox.getSelectedModule());

    if (availableBomVersions.isEmpty() && !configuredBomVersion.isPresent()) {
      hideBomUI();
    } else {
      sortBomList(availableBomVersions);

      if (availableBomVersions.size() > NUM_BOM_VERSIONS_TO_SHOW) {
        availableBomVersions = availableBomVersions.subList(0, NUM_BOM_VERSIONS_TO_SHOW);
      }

      // If there is a preconfigured BOM not already in the list, add it to the list, and re-sort
      if (configuredBomVersion.isPresent()
          && availableBomVersions.indexOf(configuredBomVersion.get()) == -1) {
        availableBomVersions.add(configuredBomVersion.get());
        sortBomList(availableBomVersions);
      }

      removeAllItems();
      availableBomVersions.forEach(this::addItem);

      // If there is a preconfigured BOM, select it by default
      if (configuredBomVersion.isPresent()) {
        setSelectedIndex(availableBomVersions.indexOf(configuredBomVersion.get()));
      }
    }
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
  void updateManagedLibraryVersionFromBom(String bomVersion) {
    if (currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
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

  private static class BomVersionRenderer extends ListCellRendererWrapper<String> {

    private static final String BOM_VERSION_DISPLAY_FORMAT = "(BOM) %s";

    @Override
    public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
      setText(String.format(BOM_VERSION_DISPLAY_FORMAT, value));
    }
  }

}
