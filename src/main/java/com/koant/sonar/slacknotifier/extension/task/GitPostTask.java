package com.koant.sonar.slacknotifier.extension.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import com.github.seratch.jslack.api.webhook.Payload;
import com.github.seratch.jslack.api.webhook.WebhookResponse;
import com.koant.sonar.slacknotifier.common.SlackNotifierProp;
import com.koant.sonar.slacknotifier.common.component.CustomProperties;

/**
 * Created by 616286 on 3.6.2016.
 * Modified by poznachowski
 */
public class GitPostTask implements PostJob {

    private static final Logger LOG = Loggers.get(GitPostTask.class);

    private Settings mSettings;
    private CustomProperties cp;
    private boolean isSending = false;
    
    public GitPostTask(CustomProperties cp) {
        LOG.info("GitPostTask----------------------------");
        this.mSettings = cp.getSettings();
        this.cp = cp;
    }
    
    @Override
    public void describe(PostJobDescriptor descriptor) {
        descriptor.name("Slack Post Task").requireProperties(
                CustomProperties.GIT_MR_IID,
                CustomProperties.GIT_PROJ_HOST, 
                CustomProperties.GIT_PROJ_ID,
                CustomProperties.GIT_SLACK_CHANNEL,
                CustomProperties.GIT_TOKEN);
    }

    @Override
    public void execute(PostJobContext context) {
        LOG.info("General22222: "+mSettings.getProperties());

        String pjfull = context.settings().getString("sonar.projectName") +" @"+ context.settings().getString("sonar.projectVersion");
        String notes = "Passed!";
        StringBuilder msg = new StringBuilder();
        List<Field> fields = new ArrayList<>();
        List<Attachment> attachments = new ArrayList<>();
        
        if (!context.analysisMode().isPublish()) {
            context.issues().forEach(new Consumer<PostJobIssue>() {
                @Override
                public void accept(PostJobIssue t) {
                    msg.append("\r\n* " + t.componentKey() + " -> Line "+t.line() + ": " + t.message());
//                    Field fd = fields.stream().filter(f->f.getTitle().equalsIgnoreCase(t.componentKey())).findAny().orElse(null);
//                    if (fd != null) {
//                        fd.setValue(fd.getValue()+"\r\n"+"  Line "+t.line()+": "+t.message());
//                    }else{
//                        fields.add(Field.builder()
//                            .title(t.componentKey())
//                            .value("  Line "+t.line()+": "+t.message())
//                            .valueShortEnough(false)
//                            .build());
//                    }
                }
            });
            
            String link = cp.projectUrl().replace(".git", "") + "/merge_requests/" + cp.mergeRequestIid();
            String pretext = "";
            if (msg.length() > 4) {
                pretext = "Analyzed Merge Request:   [!" + cp.mergeRequestIid() + "](" + link + ")";
                notes = "\r\n## Issues: \r\n" + msg.toString();
                fields.add(Field.builder()
                        .value("*Failed! Please review it!*")
                        .valueShortEnough(false)
                        .build());
                attachments.add(Attachment.builder()
                        .title(" :rage: :underage:  Review: " + pjfull)
                        .titleLink(link)
                        .fields(fields)
                        .color("danger")
                        .build());
            }else{
                pretext = "Analyzed Merge Request:  [!" + cp.mergeRequestIid() + "](" + link + ")";
                fields.add(Field.builder()
                        .value("Passed!")
                        .valueShortEnough(false)
                        .build());
                attachments.add(Attachment.builder()
                        .title(":100: :airplane: " + pjfull+"\r\n")
                        .titleLink(link)
                        .fields(fields)
                        .color("good")
                        .build());
            }
            if (cp.gitNoteEnable()) {
                sendGitNote(cp.projectHost(),
                            cp.projectId(),
                            cp.mergeRequestIid(),
                            "SonarQube: "+pjfull + "\r\n" + notes);
            }
            // Slack
            Payload payload = Payload.builder()
                    .channel(cp.slackChannel())
                    .username(context.settings().getString("ckss.user"))
                    .text(pretext)
                    .attachments(attachments)
                    .build();

            try {
                WebhookResponse response = Slack.getInstance().send(context.settings().getString(SlackNotifierProp.HOOK.property()), payload);
                if (!Integer.valueOf(200).equals(response.getCode())) {
                    LOG.error("GitFailed to post to slack, response is [{}]", response);
                }
            } catch (IOException e) {
                LOG.error("GitFailed to send slack message", e);
            }
        }
        
    }

    private void sendGitNote(String host, String pid, String mrid, String notes){
        try {
            //Create new merge request note
            //POST /projects/:id/merge_requests/:merge_request_iid/notes
            String url = ""+host+"/api/v4/projects/"+pid+"/merge_requests/"+mrid+"/notes";
           
            OkHttpClient okHttpClient = new OkHttpClient();

            RequestBody body = new FormBody.Builder()
                .add("body", ""+notes)
                .build();

            Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", this.cp.gitToken()+"")
                .post(body)
                .build();

            Call call = okHttpClient.newCall(request);
            try {
                Response response = call.execute();
                LOG.error("OKKKK to note to gitlab " + response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }catch(Exception e) {
            LOG.error("Failed to note to gitlab", e);
        }
    }

}
