import React from 'react';
import { Gantt, Task, ViewMode } from 'gantt-task-react';
import "gantt-task-react/dist/index.css";

interface GanttChartProps {
    tasks: Task[];
    compact?: boolean;
}

// Custom styles to override gantt-task-react default light theme
const ganttStyles = `
    .gantt-dark-theme .ganttTable,
    .gantt-dark-theme ._1rn3c2g,
    .gantt-dark-theme ._9w66bq,
    .gantt-dark-theme ._1nBOt,
    .gantt-dark-theme ._3lLk3 {
        background-color: #1e293b !important;
        color: #e2e8f0 !important;
    }
    .gantt-dark-theme ._WuQ0f,
    .gantt-dark-theme ._3ZbQT,
    .gantt-dark-theme ._35nLX,
    .gantt-dark-theme ._2dZTy {
        background-color: #334155 !important;
        color: #e2e8f0 !important;
        fill: #334155 !important;
    }
    .gantt-dark-theme ._3T42e,
    .gantt-dark-theme ._2B2zv,
    .gantt-dark-theme svg text {
        fill: #cbd5e1 !important;
        color: #cbd5e1 !important;
    }
    .gantt-dark-theme ._34SS0,
    .gantt-dark-theme ._3rUKi,
    .gantt-dark-theme line {
        stroke: #475569 !important;
    }
    .gantt-dark-theme ._RuwuK {
        stroke: #3b82f6 !important;
    }
    .gantt-dark-theme rect[fill="#fff"],
    .gantt-dark-theme rect[fill="white"] {
        fill: #1e293b !important;
    }
    .gantt-dark-theme ._3lLk3:hover,
    .gantt-dark-theme ._1nBOt:hover {
        background-color: #334155 !important;
    }
    .gantt-dark-theme table td,
    .gantt-dark-theme table th {
        border-color: #475569 !important;
        background-color: #1e293b !important;
        color: #e2e8f0 !important;
    }
    .gantt-dark-theme table th {
        background-color: #334155 !important;
    }
    .gantt-dark-theme ._2B2zv rect {
        fill: #1e293b !important;
    }
`;

export const GanttChart: React.FC<GanttChartProps> = ({ tasks, compact = false }) => {
    const ganttHeight = compact ? 180 : 350;
    
    return (
        <>
            <style>{ganttStyles}</style>
            <div className="w-full h-full overflow-x-auto bg-slate-800 border border-slate-700 rounded-lg shadow-lg p-4 gantt-dark-theme flex flex-col">
                <h3 className="text-sm font-semibold text-gray-100 mb-3 flex-shrink-0">Turnaround Timeline</h3>
                {tasks.length > 0 ? (
                    <div className="gantt-dark-theme flex-1 overflow-auto">
                        <Gantt
                            tasks={tasks}
                            viewMode={ViewMode.Hour}
                            columnWidth={55}
                            listCellWidth="130px"
                            barFill={65}
                            ganttHeight={ganttHeight}
                            rowHeight={36}
                            fontSize="12px"
                            todayColor="rgba(59, 130, 246, 0.15)"
                            barCornerRadius={4}
                        />
                    </div>
                ) : (
                    <div className="text-gray-400 text-center py-6 text-sm">No tasks available</div>
                )}
            </div>
        </>
    );
};
