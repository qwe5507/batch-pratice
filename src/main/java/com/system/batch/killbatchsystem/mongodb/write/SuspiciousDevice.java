package com.system.batch.killbatchsystem.mongodb.write;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Data
public class SuspiciousDevice {
    @Id
    private String id;
    private String macAddress;
    private String deviceName;
    private String location;
    private Date timestamp;
}

