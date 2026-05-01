import React from 'react';
import '../MigrateDB.css';

const StatCard = ({ label, value, sub, color }) => {
    return (
        <div className="stat-card">
            <div className="stat-label">
                {label}
            </div>

            <div className={`stat-value ${color}`}>
                {value}
            </div>

            <div className="stat-sub">
                {sub}
            </div>
        </div>
    );
};

export default StatCard;