package com.koant.sonar.slacknotifier.common.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.koant.sonar.slacknotifier.common.SlackNotifierProp;

/**
 * Abstract base component for Slack notifying Sonar extensions.
 * Concrete implementations must call com.koant.sonar.slacknotifier.common.component.AbstractSlackNotifyingComponent#refreshSettings() in the beginning of actual execution.
 */
public abstract class AbstractSlackNotifyingComponent {

    private static final Logger LOG = Loggers.get(AbstractSlackNotifyingComponent.class);

    private Settings settings;
    private List<Map<String, ProjectConfig>> projectConfigMap = Collections.emptyList();

    public AbstractSlackNotifyingComponent(CustomProperties cp){this.settings = cp.getSettings();}
    public AbstractSlackNotifyingComponent(Settings settings){ this.settings = settings;}
    
    public void init(Settings settings) {
        this.settings = settings;
        LOG.info("Init called, project slack channel config map constructed from general settings");
    }

    /**
     * This method has to be called in the beginning of every actual plugin execution.
     * SonarQube seems to work in such a way that
     * <pre>
     * 1) the Settings object is constructor injected to this class.
     * 2) the values reflected by the Settings object reflect latest settings configured
     * 3) but the constructor of this class is called only once, and after that the class is never instantiated again (the same instance is reused)
     * 4) thus when the instance is used to perform something, we must refresh the projectConfigMap when the execution starts
     * </pre>
     */
    protected void refreshSettings() {
        LOG.info("Refreshing settings");
        refreshProjectConfigs();
    }

    private void refreshProjectConfigs() {
        LOG.info("Refreshing project configs");
        this.projectConfigMap = buildProjectConfigByProjectKeyMap(settings);
    }

    protected String getSlackIncomingWebhookUrl() {
        return settings.getString(SlackNotifierProp.HOOK.property());
    }

    protected String getSlackUser() {
        return settings.getString(SlackNotifierProp.USER.property());
    }

    protected boolean isPluginEnabled() {
        return settings.getBoolean(SlackNotifierProp.ENABLED.property());
    }
    

    /**
     * Returns the sonar server url, with a trailing /
     *
     * @return
     */
    protected String getSonarServerUrl() {
        String u = settings.getString(SlackNotifierProp.SONAR.property());
        if (u == null || u.length() <= 0) {
            u = settings.getString("sonar.core.serverBaseURL");
        }
        if (u == null) {
            return null;
        }
        if (u.endsWith("/")) {
            return u;
        }
        return u + "/";
    }

    protected List<Optional<ProjectConfig>> getProjectConfig(String projectKey) {
        ArrayList<Optional<ProjectConfig>> ret = new ArrayList<Optional<ProjectConfig>>();
    	for (Map<String, ProjectConfig> m : projectConfigMap) {
            List<ProjectConfig> projectConfigs = m.keySet()
                    .stream()
                    .filter(key -> key.endsWith("*") ? projectKey.startsWith(key.substring(0, key.length() - 1))
                            : key.equals(projectKey))
                    .map(m::get)
                    .collect(Collectors.toList());
            // Not configured at all
            if (projectConfigs.isEmpty()) {
                LOG.info("Could not find config for project [{}] in [{}]", projectKey, projectConfigMap);
                ret.add(Optional.empty());
            }else {
            	for (ProjectConfig pc : projectConfigs) {
            		ret.add(Optional.of(pc));
            	}
            }
    	}
        return ret;
    }

    private static List<Map<String, ProjectConfig>> buildProjectConfigByProjectKeyMap(Settings settings) {
    	List<Map<String, ProjectConfig>> list = new ArrayList<>();
    	
        String[] projectConfigIndexes = settings.getStringArray(SlackNotifierProp.CONFIG.property());

        for (String projectConfigIndex : projectConfigIndexes) {
            Map<String, ProjectConfig> map = new HashMap<>();
            String projectKeyProperty = SlackNotifierProp.CONFIG.property() + "." + projectConfigIndex + "." + SlackNotifierProp.PROJECT.property();
            String projectKey = settings.getString(projectKeyProperty);
            if (projectKey == null) {
                throw MessageException.of("Slack notifier configuration is corrupted. At least one project specific parameter has no project key. " +
                        "Contact your administrator to update this configuration in the global administration section of SonarQube.");
            }
            ProjectConfig value = ProjectConfig.create(settings, projectConfigIndex);
            LOG.info("Found project configuration [{}]", value);
            map.put(projectKey, value);
            list.add(map);
        }
        return list;
    }

    protected String logRelevantSettings() {
        Map<String, String> pluginSettings = new HashMap<>();
        mapSetting(pluginSettings, SlackNotifierProp.HOOK);
        mapSetting(pluginSettings, SlackNotifierProp.USER);
        mapSetting(pluginSettings, SlackNotifierProp.ENABLED);
        mapSetting(pluginSettings, SlackNotifierProp.CONFIG);
        return pluginSettings.toString() + "; project specific channel config: " + projectConfigMap;
    }

    private void mapSetting(Map<String, String> pluginSettings, SlackNotifierProp key) {
        pluginSettings.put(key.name(), settings.getString(key.property()));
    }

    protected boolean shouldSkipSendingNotification(ProjectConfig projectConfig, QualityGate qualityGate) {
        // Disabled due to missing channel value
        if (projectConfig.getSlackChannel() == null ||
                "".equals(projectConfig.getSlackChannel().trim())) {
            LOG.info("Slack channel for project [{}] is blank, notifications disabled", projectConfig.getProjectKey());
            return true;
        }
        if (projectConfig.isQgFailOnly() && qualityGate != null && QualityGate.Status.OK.equals(qualityGate.getStatus())) {
            LOG.info("Project [{}] set up to send notification on failed Quality Gate, but was: {}", projectConfig.getProjectKey(), qualityGate.getStatus().name());
            return true;
        }
        return false;
    }
}
