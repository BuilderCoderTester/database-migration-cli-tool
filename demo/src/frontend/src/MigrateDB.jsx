import React, { useEffect, useState } from 'react';
import {
    Search, Plus, ShieldCheck,
    RotateCcw, Wrench, ArrowDownToLine
} from 'lucide-react';

import './MigrateDB.css';

import Sidebar from './components/Sidebar';
import TopNav from './components/TopNav';
import StatCard from './components/StatCard';
import MigrationTable from './components/MigrationTable';
import ActivityLog from './components/ActivityLog';

const API = "http://localhost:8080/api/migrations";

const MigrateDB = () => {
    const [activeTab, setActiveTab] = useState('Migrations');
    const [searchQuery, setSearchQuery] = useState('');
    const [stats, setStats] = useState([]);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(false);

    // 🔥 LOAD DATA
    const loadData = async () => {
        try {
            const historyRes = await fetch(`${API}/history`);
            const historyData = await historyRes.json();

            const statusRes = await fetch(`${API}/status`);
            const statusData = await statusRes.json();

            setHistory(historyData);

            const total = historyData.length;
            const applied = historyData.filter(m => m.success).length;
            const failed = historyData.filter(m => !m.success).length;
            const pending = statusData.pendingCount || 0;

            setStats([
                { label: 'Total', value: total, sub: 'migrations found', color: 'stat-blue' },
                { label: 'Applied', value: applied, sub: 'successfully run', color: 'stat-green' },
                { label: 'Pending', value: pending, sub: 'awaiting execution', color: 'stat-amber' },
                { label: 'Failed', value: failed, sub: 'errors', color: 'stat-red' },
                { label: 'Avg Duration', value: '—', sub: 'per migration', color: 'stat-neutral' },
            ]);

        } catch (err) {
            console.error("Failed to load data", err);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    // 🔥 ACTIONS
    const handleMigrate = async () => {
        try {
            setLoading(true);

            await fetch(`${API}/migrate`, { method: "POST" });

            await loadData();
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const handleRollback = async () => {
        await fetch(`${API}/rollback`, { method: "POST" });
        loadData();
    };

    const handleValidate = async () => {
        await fetch(`${API}/validate`, { method: "POST" });
    };

    const handleRepair = async () => {
        await fetch(`${API}/repair`, { method: "POST" });
        loadData();
    };

    return (
        <div className="migrate-container">
            <TopNav activeTab={activeTab} setActiveTab={setActiveTab} />

            <div className="layout">
                <Sidebar />

                <main className="main-content">

                    {/* Toolbar */}
                    <div className="topnav">
                        <span className="toolbar-title">Migration Files</span>

                        <ToolbarButton icon={<Plus size={14}/>} label="New Migration" />
                        <ToolbarButton icon={<ShieldCheck size={14}/>} label="Validate" onClick={handleValidate} />
                        <ToolbarButton icon={<RotateCcw size={14}/>} label="Rollback" variant="red" onClick={handleRollback} />

                        <div className="spacer" />

                        <ToolbarButton icon={<Wrench size={14}/>} label="Repair" variant="green" onClick={handleRepair} />
                        <ToolbarButton
                            icon={<ArrowDownToLine size={14}/>}
                            label={loading ? "Migrating..." : "Migrate Now"}
                            variant="blue"
                            onClick={handleMigrate}
                        />
                    </div>

                    {/* Stats */}
                    <div className="stats-grid">
                        {stats.map((stat, i) => (
                            <StatCard key={i} {...stat} />
                        ))}
                    </div>

                    {/* Content */}
                    <div className="content-area">
                        <div className="content-header">
                            <span>All Migrations</span>

                            <span className="badge">
                                {history.length} total
                            </span>

                            <div className="search-box">
                                <Search size={14} className="search-icon" />
                                <input
                                    placeholder="Search..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                />
                            </div>
                        </div>

                        {/* 🔥 PASS REAL DATA */}
                        <MigrationTable
                            searchQuery={searchQuery}
                            data={history}
                        />
                    </div>

                    {/* Footer */}
                    <div className="footer">
                        <span>Migration progress</span>

                        <div className="progress-bar">
                            <div
                                className="progress-fill"
                                style={{
                                    width: stats.length
                                        ? `${(stats[1]?.value / stats[0]?.value) * 100 || 0}%`
                                        : "0%"
                                }}
                            />
                        </div>

                        <span>
                            {stats[1]?.value || 0} / {stats[0]?.value || 0} applied
                        </span>
                    </div>

                </main>

                <ActivityLog />
            </div>
        </div>
    );
};

const ToolbarButton = ({ icon, label, variant, onClick }) => {
    return (
        <button onClick={onClick} className={`toolbar-btn ${variant || ''}`}>
            {icon} {label}
        </button>
    );
};

export default MigrateDB;