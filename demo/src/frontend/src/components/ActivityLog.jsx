import React, { useEffect, useState } from 'react';
import '../MigrateDB.css';

const API = "http://localhost:8080/api/migrations";

const ActivityLog = () => {
    const [logs, setLogs] = useState([]);

    // 🔥 FETCH LOGS (polling every 2s)
    useEffect(() => {
        const fetchLogs = async () => {
            try {
                const res = await fetch(`${API}/logs`);
                const data = await res.json();
                setLogs(data);
            } catch (err) {
                console.error("Failed to load logs", err);
            }
        };

        fetchLogs();
        const interval = setInterval(fetchLogs, 2000);

        return () => clearInterval(interval);
    }, []);

    return (
        <aside className="activity-log">

            <div className="activity-header">
                Activity Log
            </div>

            <div className="activity-body">

                {/* 🔥 LOGS */}
                {logs.length === 0 && (
                    <div className="log-empty">
                        No activity yet
                    </div>
                )}

                {logs.map((log, i) => (
                    <div key={i} className="log-item">

                        {/* Dot color based on type */}
                        <div className={`log-dot ${getDotClass(log.level)}`} />

                        <div>
                            <div className="log-text">
                                {log.message}
                            </div>

                            <div className="log-time">
                                {formatTime(log.timestamp)}
                            </div>
                        </div>
                    </div>
                ))}

                {/* Database Info */}
                <div className="info-box">
                    <InfoRow label="Host" value="prod-postgres-01" />
                    <InfoRow label="Database" value="app_production" />
                    <InfoRow label="Port" value="5432" />
                </div>

                {/* Schema */}
                <div className="schema-box">
                    <div className="schema-title">
                        Schema history table
                    </div>
                    <div className="schema-value">
                        flyway_schema_history
                    </div>
                </div>

            </div>
        </aside>
    );
};

const InfoRow = ({ label, value }) => (
    <div className="info-row">
        <span className="info-label">{label}</span>
        <span className="info-value">{value}</span>
    </div>
);

export default ActivityLog;



// 🔥 HELPER: map log level → color
const getDotClass = (level) => {
    switch (level) {
        case "SUCCESS":
            return "dot-green";
        case "ERROR":
            return "dot-red";
        case "WARN":
            return "dot-amber";
        default:
            return "dot-blue";
    }
};

// 🔥 HELPER: format time
const formatTime = (timestamp) => {
    if (!timestamp) return "-";
    const date = new Date(timestamp);
    return date.toLocaleString();
};