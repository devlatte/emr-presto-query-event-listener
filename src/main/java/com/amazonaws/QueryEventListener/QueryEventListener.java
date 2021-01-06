/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.QueryEventListener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.facebook.presto.spi.eventlistener.EventListener;
import com.facebook.presto.spi.eventlistener.QueryCompletedEvent;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.ObjectMapper;

class ExtendableLog {
    public String log_type;
    public long create_time;
    private Map<String, String> properties;

    @JsonAnyGetter
    public Map<String, String> getProperties() {
        return properties;
    }

    public ExtendableLog(String log_type, long create_time) {
        this.log_type = log_type;
        this.create_time = create_time;
        this.properties = new HashMap<String, String>();
    }
    public void put(String k, String v) {
        properties.put(k,v);
    }

}

public class QueryEventListener
        implements EventListener
{
    private static final Logger logger = LogManager.getLogger(QueryEventListener.class);


    public void queryCompleted(QueryCompletedEvent queryCompletedEvent)
    {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ");
        String errorCode = null;
        ExtendableLog log;

        try {
            errorCode = queryCompletedEvent.getFailureInfo().get().getErrorCode().getName().toString();
        }
        catch (NoSuchElementException noElEx) {

        }
        if (errorCode != null) {
            log = new ExtendableLog("COMPLETED_ERROR", queryCompletedEvent.getCreateTime().toEpochMilli());
            log.put("error_code", errorCode);
        } else {
            log = new ExtendableLog("COMPLETED", queryCompletedEvent.getCreateTime().toEpochMilli());
        }
        try {
            log.put("query_id", queryCompletedEvent.getMetadata().getQueryId().toString());
            log.put("user", queryCompletedEvent.getContext().getUser().toString());
            log.put("create_time_str", queryCompletedEvent.getCreateTime().atZone(ZoneId.systemDefault()).format(df));
            log.put("start_time_str", queryCompletedEvent.getExecutionStartTime().atZone(ZoneId.systemDefault()).format(df));
            log.put("end_time_str", queryCompletedEvent.getEndTime().atZone(ZoneId.systemDefault()).format(df));
            log.put("query", queryCompletedEvent.getMetadata().getQuery());
            log.put("complete", Boolean.toString(queryCompletedEvent.getStatistics().isComplete()));
            log.put("catalog", queryCompletedEvent.getContext().getCatalog().orElse(""));
            log.put("schema", queryCompletedEvent.getContext().getSchema().orElse(""));
            log.put("remote_client_address", queryCompletedEvent.getContext().getRemoteClientAddress().orElse(""));
            log.put("source", queryCompletedEvent.getContext().getSource().orElse(""));
            log.put("server_address", queryCompletedEvent.getContext().getServerAddress());

            String jsonStr = new ObjectMapper().writeValueAsString(log);
            logger.info(jsonStr);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
