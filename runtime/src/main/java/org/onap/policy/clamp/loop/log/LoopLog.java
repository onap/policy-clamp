/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.policy.clamp.loop.log;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.onap.policy.clamp.loop.Loop;

/**
 * This class holds the logs created by the Clamp Backend. The Instant is always
 * rounded to the nearest second as the nano seconds can't be stored in the
 * database. The logs can be therefore exposed to the UI or the client doing
 * some GET Loop on the backend.
 *
 */
@Entity
@Table(name = "loop_logs")
public class LoopLog implements Serializable, Comparable<LoopLog> {
    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 1988276670074437631L;

    @Expose
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Expose
    @Column(name = "log_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private LogType logType;

    @Expose
    @Column(name = "log_component", nullable = false)
    private String logComponent;

    @Expose
    @Column(name = "message", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_id", nullable = false)
    private Loop loop;

    @Expose
    @Column(name = "log_instant", nullable = false)
    private Instant logInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    public LoopLog() {
    }

    /**
     * Constructor For LoopLog taking message and logtype, logComponent and loop
     * reference.
     * 
     * @param message      The message as string
     * @param logType      Type like INFO, WARN, DEBUG
     * @param logComponent A String with DCAE, POLICY, CLAMP ,etc...
     * @param loop         The loop object that this log is about
     */
    public LoopLog(String message, LogType logType, String logComponent, Loop loop) {
        this.message = message;
        this.logType = logType;
        this.loop = loop;
        this.logComponent = logComponent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(LogType logType) {
        this.logType = logType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Loop getLoop() {
        return loop;
    }

    public void setLoop(Loop loop) {
        this.loop = loop;
    }

    public Instant getLogInstant() {
        return logInstant;
    }

    public void setLogInstant(Instant logInstant) {
        this.logInstant = logInstant.truncatedTo(ChronoUnit.SECONDS);
    }

    public String getLogComponent() {
        return logComponent;
    }

    public void setLogComponent(String logComponent) {
        this.logComponent = logComponent;
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LoopLog other = (LoopLog) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(LoopLog arg0) {
        // Reverse it, so that by default we have the latest
        if (getId() == null) {
            return 1;
        }
        if (arg0.getId() == null) {
            return -1;
        }
        return arg0.getId().compareTo(this.getId());
    }

}
