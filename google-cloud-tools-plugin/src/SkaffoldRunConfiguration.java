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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.SearchScopeProvider;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import java.io.File;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkaffoldRunConfiguration extends RunConfigurationBase {

  SkaffoldRunConfiguration(
      @NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
    super(project, factory, name);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new SettingsEditor<RunConfiguration>() {
      @Override
      protected void resetEditorFrom(@NotNull RunConfiguration s) {}

      @Override
      protected void applyEditorTo(@NotNull RunConfiguration s) throws ConfigurationException {}

      @NotNull
      @Override
      protected JComponent createEditor() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new MigLayout());

        jPanel.add(new JLabel("Skaffold configuration:"));
        jPanel.add(
            new JComboBox<>(new String[] {"k8s/skaffold/skaffold.yaml"}), "pushx, growx, wrap");

        jPanel.add(new JLabel("Deployment Settings"), "gaptop 12, split 2, span 3");
        jPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "gaptop 12, growx, wrap");
        jPanel.add(
            new JRadioButton("Deploy continuously on each project build", true),
            "gapleft 20, span 2, wrap");
        jPanel.add(new JRadioButton("Deploy once", false), "gapleft 20, span 2, wrap");

        return jPanel;
      }
    };
  }

  @Nullable
  @Override
  public RunProfileState getState(
      @NotNull Executor executor, @NotNull ExecutionEnvironment environment) {

    return new SkaffoldCommandLineRunState(environment);
  }

  private static class SkaffoldCommandLineRunState extends CommandLineState {

    private SkaffoldCommandLineRunState(ExecutionEnvironment environment) {
      super(environment);
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
      try {
        String[] commandLine = { "skaffold", "dev" };
        File projectDir = new File(getEnvironment().getProject().getBaseDir().getPath());
        ProcessBuilder processBuilder = new ProcessBuilder();
        Map<String,String> envMap = processBuilder.environment();
        envMap.put("PATH", envMap.get("PATH") + File.pathSeparator + "/usr/local/google/home/ivanporty/google-cloud-sdk/bin");
        Process process = processBuilder.directory(projectDir).command(commandLine).start();

        // another console or the same one?
        final GlobalSearchScope searchScope = SearchScopeProvider
            .createSearchScope(getEnvironment().getProject(), getEnvironment().getRunProfile());
        TextConsoleBuilder customConsole = TextConsoleBuilderFactory.getInstance().createBuilder(getEnvironment().getProject(), searchScope);
        ConsoleView view = customConsole.getConsole();
        view.print("This is another console!", ConsoleViewContentType.NORMAL_OUTPUT);
        // getConsoleBuilder().getConsole().print("Hello from the same console here!", ConsoleViewContentType.NORMAL_OUTPUT);

        return new OSProcessHandler(process, String.join(" ", commandLine));
      } catch (Exception ex) {
        throw new ExecutionException(ex);
      }
    }
  }
}
