package kollus.stmp.stmp.component;

import kollus.stmp.stmp.dao.DbGroupReservationEntity;
import kollus.stmp.stmp.dao.DbGroupReservationRepository;
import kollus.stmp.stmp.dao.DbReservationEntity;
import kollus.stmp.stmp.dao.DbReservationRepository;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
public class ScheduleComponent{

    @Autowired
    private DbReservationRepository dbReservationRepository;
    @Autowired
    private DbGroupReservationRepository dbGroupReservationRepository;

    public String getHTMLMailForm(String textBody){

        String mailForm = "<div style = 'width: 900px;'>";
        mailForm+=textBody;
        mailForm+="</div>";

        return mailForm;
    }

    public String getGroupCodeForm(){
        return dbGroupReservationRepository.getGroupCode();
    }

    public void setGroupTableData(String groupCode, String title){
        DbGroupReservationEntity dbGroupReservationEntity = new DbGroupReservationEntity();
        dbGroupReservationEntity.setGroup_code(groupCode);
        dbGroupReservationEntity.setGroup_title(title);
        Date dt = new Date();
        dbGroupReservationEntity.setReservation_date(dt);
        dbGroupReservationEntity.setState("R");

        dbGroupReservationRepository.save(dbGroupReservationEntity);
    }
    public void setReservationTableData(List<HashMap<String, String>> customerlist, String groupCode,String emailTitle, String sendBody){
        for(HashMap<String, String> items : customerlist){
            UUID uuid = UUID.randomUUID();
            String jobKeyName = Long.toString(uuid.getLeastSignificantBits(), 36).replace('-', 'C');
            String sendTitle = "["+items.get("customerNM")+"]"+emailTitle;

            DbReservationEntity dbReservationEntity = new DbReservationEntity();
            dbReservationEntity.setEmail_title(sendTitle);
            dbReservationEntity.setCustomer_address(items.get("customerEmail"));
            dbReservationEntity.setEmail_form(sendBody);
            dbReservationEntity.setGroup_code(groupCode);
            dbReservationEntity.setReservation_code(jobKeyName);
            dbReservationEntity.setState("R");

            dbReservationRepository.save(dbReservationEntity);
        }
    }

    public JobDetail buildJobDetail(String groupCode) {

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("groupCode", groupCode);

        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(groupCode, "email-jobs")
                .withDescription("Send Email Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    public Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "email-triggers")
                .withDescription("Send Email Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }

}