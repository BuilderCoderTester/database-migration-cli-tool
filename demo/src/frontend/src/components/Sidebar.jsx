import {
    Database, LayoutGrid, Clock, CheckCircle2,
    Settings, Activity
} from 'lucide-react';
import React from 'react';
import '../MigrateDB.css';

const Sidebar = () => (
    <aside className="sidebar">

        <div className="sidebar-section-title">Views</div>

        <SidebarItem icon={<LayoutGrid size={15}/>} label="All Migrations" active />
        <SidebarItem icon={<Clock size={15}/>} label="Pending" badge="3" />
        <SidebarItem icon={<CheckCircle2 size={15}/>} label="Applied" />
        <SidebarItem icon={<Activity size={15}/>} label="Run History" />

        <div className="sidebar-section-title mt">Config</div>

        <SidebarItem icon={<Database size={15}/>} label="Connections" />
        <SidebarItem icon={<Settings size={15}/>} label="Settings" />

    </aside>
);

export default Sidebar;


const SidebarItem = ({ icon, label, active, badge }) => (
    <div className={`sidebar-item ${active ? 'active' : ''}`}>
        {icon}
        <span className="sidebar-label">{label}</span>

        {badge && (
            <span className="sidebar-badge">
                {badge}
            </span>
        )}
    </div>
);