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

package com.google.cloud.tools.intellij.appengine.java.startup;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardWebIntegration;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService.SdkStatusUpdateListener;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkServiceManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;

/** Run at plugin startup to do App Engine specific plugin initialization. */
public class AppEngineInitializationComponent implements ApplicationComponent {

  @Override
  public void initComponent() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      CloudSdkService cloudSdkService = ServiceManager.getService(CloudSdkServiceManager.class)
          .getCloudSdkService();
      cloudSdkService.activate();
      cloudSdkService.addStatusUpdateListener(
          new SdkStatusUpdateListener() {
            @Override
            public void onSdkStatusChange(CloudSdkService sdkService, SdkStatus status) {
              if (status == SdkStatus.READY) {
                AppEngineStandardWebIntegration.getInstance().setupDevServer();
              }
            }

            @Override
            public void onSdkProcessingStarted() {}
          });
    }
  }
}
