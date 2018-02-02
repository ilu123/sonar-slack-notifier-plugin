package com.koant.sonar.slacknotifier.extension.task;

import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.koant.sonar.slacknotifier.common.component.CustomProperties;

/**
 * Created by 616286 on 3.6.2016.
 * Modified by poznachowski
 */
public class SlackPostProjectAnalysisTask extends ProjectBuilder{

    private static final Logger LOG = Loggers.get(SlackPostProjectAnalysisTask.class);

    private CustomProperties cp;
    
    public SlackPostProjectAnalysisTask(CustomProperties cp) {
        super();
        
        LOG.info("pb-------:"+cp.mergeRequestIid());
        this.cp = cp;
    }
    
    @Override
    public void build(Context context) {
        LOG.info("pb2-------:"+this.cp);
    }
}
