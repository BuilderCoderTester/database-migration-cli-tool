import React from 'react';
import { CheckCircle2 } from 'lucide-react';
import '../MigrateDB.css';

const MigrationTable = ({ searchQuery, data = [] }) => {

    // 🔍 Filter using backend fields
    const filteredData = data.filter(item =>
        item.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.version?.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="table-container">
            <table className="migration-table">

                <thead>
                <tr>
                    <th className="col-icon"></th>
                    <th>Version</th>
                    <th>Description</th>
                    <th>Applied On</th>
                    <th>Duration</th>
                    <th>Status</th>
                </tr>
                </thead>

                <tbody>

                {filteredData.length === 0 && (
                    <tr>
                        <td colSpan="6" style={{ textAlign: 'center', padding: '20px', color: '#555d6e' }}>
                            No migrations found
                        </td>
                    </tr>
                )}

                {filteredData.map((row, i) => (
                    <tr key={i} className="table-row">

                        {/* Status Icon */}
                        <td className="cell-icon">
                            {row.success
                                ? <CheckCircle2 size={14} className="icon-success" />
                                : <div className="dot-pending" />
                            }
                        </td>

                        {/* Version */}
                        <td className="mono text-dim">
                            {row.version ? `V${row.version}` : '-'}
                        </td>

                        {/* Description */}
                        <td className="text-main">
                            {row.description || '-'}
                        </td>

                        {/* Applied Date */}
                        <td className="mono text-muted">
                            {row.executedAt || '-'}
                        </td>

                        {/* Duration */}
                        <td className="mono text-muted">
                            {row.executionTime ? `${row.executionTime} ms` : '-'}
                        </td>

                        {/* Status */}
                        <td>
                            <span className={`badge ${row.success ? 'badge-green' : 'badge-amber'}`}>
                                {row.success ? 'Applied' : 'Failed'}
                            </span>
                        </td>

                    </tr>
                ))}

                </tbody>

            </table>
        </div>
    );
};

export default MigrationTable;