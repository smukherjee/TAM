import React from 'react';
import { Gantt, Task, ViewMode } from 'gantt-task-react';
import "gantt-task-react/dist/index.css";

interface GanttChartProps {
    tasks: Task[];
}

export const GanttChart: React.FC<GanttChartProps> = ({ tasks }) => {
    return (
        <div className="w-full overflow-x-auto bg-white rounded-lg shadow p-4">
            <h3 className="text-lg font-semibold mb-4">Turnaround Timeline</h3>
            {tasks.length > 0 ? (
                <Gantt
                    tasks={tasks}
                    viewMode={ViewMode.Hour}
                    columnWidth={60}
                    listCellWidth="155px"
                    barFill={60}
                    ganttHeight={300}
                />
            ) : (
                <div className="text-gray-500 text-center py-8">No tasks available</div>
            )}
        </div>
    );
};
