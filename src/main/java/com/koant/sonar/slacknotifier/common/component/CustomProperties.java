package com.koant.sonar.slacknotifier.common.component;

import static java.util.Arrays.asList;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class CustomProperties {
    public static final String GIT_PROJ_HOST = "sonar.cks.project.host";
    public static final String GIT_MR_IID = "sonar.cks.mr.iid";
    public static final String GIT_PROJ_ID = "sonar.cks.project.id";
    public static final String GIT_TOKEN = "sonar.cks.git.token";
    public static final String GIT_SLACK_CHANNEL = "sonar.cks.mr.channel";
    
    public static final String CATEGORY = "Slack";
    public static final String SUB_CATEGORY = "Git";

    private final Settings mSettings;
    
    public CustomProperties(Settings settings) {
        super();
        
        mSettings = settings;
    }
    
    public Settings getSettings(){
        return this.mSettings;
    }
    
    public String projectHost() {
        return this.mSettings.getString(GIT_PROJ_HOST);
    }
    
    public String mergeRequestIid() {
        return this.mSettings.getString(GIT_MR_IID);
    }

    public String slackChannel() {
        return this.mSettings.getString(GIT_SLACK_CHANNEL);
    }

    public String projectId() {
        return this.mSettings.getString(GIT_PROJ_ID);
    }
    
    public String gitToken(){
        return this.mSettings.getString(GIT_TOKEN);
    }

    public static List<PropertyDefinition> getProperties() {
      return asList(
        PropertyDefinition.builder(GIT_PROJ_HOST)
        .category(CATEGORY)
          .name(GIT_PROJ_HOST)
          .subCategory(SUB_CATEGORY)
          .defaultValue("")
          .description("")
          .onQualifiers(Qualifiers.PROJECT)
          .build(),
          PropertyDefinition.builder(GIT_TOKEN)
          .category(CATEGORY)
            .name(GIT_TOKEN)
            .subCategory(SUB_CATEGORY)
            .defaultValue("")
            .description("")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
            PropertyDefinition.builder(GIT_SLACK_CHANNEL)
            .category(CATEGORY)
              .name(GIT_SLACK_CHANNEL)
              .subCategory(SUB_CATEGORY)
              .defaultValue("")
              .description("")
              .onQualifiers(Qualifiers.PROJECT)
              .build(),
        PropertyDefinition.builder(GIT_MR_IID)
          .category(CATEGORY)
          .name(GIT_MR_IID)
          .subCategory(SUB_CATEGORY)
          .defaultValue("")
          .description("")
          .onQualifiers(Qualifiers.PROJECT)
          .build(),
        PropertyDefinition.builder(GIT_PROJ_ID)
          .category(CATEGORY)
          .name(GIT_PROJ_ID)
          .subCategory(SUB_CATEGORY)
          .defaultValue("")
          .description("")
          .onQualifiers(Qualifiers.PROJECT)
          .build()
      );
    }
}
