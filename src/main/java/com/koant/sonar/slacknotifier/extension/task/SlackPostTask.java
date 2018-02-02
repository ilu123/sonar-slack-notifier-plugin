package com.koant.sonar.slacknotifier.extension.task;

import java.io.IOException;
import java.util.Optional;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.webhook.Payload;
import com.github.seratch.jslack.api.webhook.WebhookResponse;
import com.koant.sonar.slacknotifier.common.component.AbstractSlackNotifyingComponent;
import com.koant.sonar.slacknotifier.common.component.CustomProperties;
import com.koant.sonar.slacknotifier.common.component.ProjectConfig;

/**
 * Created by 616286 on 3.6.2016.
 * Modified by poznachowski
 */
public class SlackPostTask extends AbstractSlackNotifyingComponent implements PostProjectAnalysisTask {

    private static final Logger LOG = Loggers.get(SlackPostTask.class);

    private Settings mSettings;
    private final I18n i18n;
    private final Slack slackClient;
    private final CustomProperties cp;
    
    public SlackPostTask(Settings settings, I18n i18n) {
        super(settings);
        this.mSettings = settings;
        this.i18n = i18n;
        this.slackClient = Slack.getInstance();
        this.cp = new CustomProperties(mSettings);
    }

    @Override
    public void finished(ProjectAnalysis analysis) {

        LOG.info("MRIID: "+this.cp.mergeRequestIid());
        LOG.info("General: "+this.mSettings.getString("ckss.hook"));

        
        refreshSettings();
        
        if (!isPluginEnabled()) {
            LOG.info("Slack notifier plugin disabled, skipping. Settings are [{}]", logRelevantSettings());
            return;
        }

        String projectKey = analysis.getProject().getKey();

        Optional<ProjectConfig> projectConfigOptional = getProjectConfig(projectKey);
        if (!projectConfigOptional.isPresent()) {
            return;
        }

        ProjectConfig projectConfig = projectConfigOptional.get();
        if (shouldSkipSendingNotification(projectConfig, analysis.getQualityGate())) {
            return;
        }
        
        LOG.info("Slack notification will be sent: " + analysis.toString());
        
        Payload payload = ProjectAnalysisPayloadBuilder.of(analysis)
                .i18n(i18n)
                .projectConfig(projectConfig)
                .projectUrl(projectUrl(projectKey))
                .username(getSlackUser())
                .build();
    
        try {
            // See https://github.com/seratch/jslack
            WebhookResponse response = slackClient.send(getSlackIncomingWebhookUrl(), payload);
            if (!Integer.valueOf(200).equals(response.getCode())) {
                LOG.error("Failed to post to slack, response is [{}]", response);
            }
        } catch (IOException e) {
            LOG.error("Failed to send slack message", e);
        }
    }

    private String projectUrl(String projectKey) {
        return getSonarServerUrl() + "dashboard?id=" + projectKey;
    }


}
