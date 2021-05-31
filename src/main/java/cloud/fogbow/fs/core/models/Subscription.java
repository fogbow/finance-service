package cloud.fogbow.fs.core.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_subscription_table")
public class Subscription {
    private static final String START_TIME_COLUMN_NAME = "start_time";
    private static final String END_TIME_COLUMN_NAME = "end_time";
    private static final String PLAN_NAME_COLUMN_NAME = "plan_name";

    @Id
    private String id;
    
    @Column(name = START_TIME_COLUMN_NAME)
    private Long startTime;
    
    @Column(name = END_TIME_COLUMN_NAME)
    private Long endTime;
    
    @Column(name = PLAN_NAME_COLUMN_NAME)
    private String planName;
    
    public Subscription() {
        
    }
    
    public Subscription(String id, Long startTime, String planName) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = -1L;
        this.planName = planName;
    }
    
    public Subscription(String id, Long startTime, Long endTime, String planName) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.planName = planName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }
}
