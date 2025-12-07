import React, { useEffect, useState } from 'react';
import { getTurnaroundEvents } from '../../services/api';

interface TurnaroundEvent {
  eventUniqueId: string;
  cameraId: string;
  cameraName: string;
  activityType: string;
  eventType: number; // 0 = Start, 1 = Stop
  eventTimeStamp: string;
  stand: string;
}

interface GanttTask {
  activity: string;
  start: Date;
  end?: Date;
  duration?: number; // in minutes
  color: string;
}

const normalizeActivity = (name: string) => name.toLowerCase().trim();

const ACTIVITY_CONFIG: { [key: string]: { color: string, label: string } } = {
  "passenger boarding bridge": { color: "#3b82f6", label: "Passenger Boarding Bridge" },
  "passenger step ladder front": { color: "#ef4444", label: "Passenger Step Ladder Front" },
  "passenger step ladder back": { color: "#a855f7", label: "Passenger Step Ladder Back" },
  "towable conveyor belt front": { color: "#ec4899", label: "Towable Conveyor Belt Front" },
  "towable conveyor belt back": { color: "#8b5cf6", label: "Towable Conveyor Belt Back" },
  "aircraft back door": { color: "#6366f1", label: "Aircraft Back Door" },
  "aircraft front door": { color: "#14b8a6", label: "Aircraft Front Door" },
  "aircraft front belly": { color: "#f43f5e", label: "Aircraft Front Belly" },
  "aircraft back belly": { color: "#f97316", label: "Aircraft Back Belly" },
  "passenger coach": { color: "#ef4444", label: "Passenger Coach" },
  "person arrival movement": { color: "#f59e0b", label: "Person Arrival Movement" },
  "person departure movement": { color: "#10b981", label: "Person Departure Movement" },
  "head unit (ebt) / diesel tug (dt)": { color: "#0ea5e9", label: "Head Unit (EBT) / Diesel Tug (DT)" },
  "head unit ebt diesel tug dt": { color: "#0ea5e9", label: "Head Unit (EBT) / Diesel Tug (DT)" },
  "bag movement arrival": { color: "#f43f5e", label: "Bag Movement Arrival" },
  "bag movement departure": { color: "#3b82f6", label: "Bag Movement Departure" },
  "fuel vehicle": { color: "#10b981", label: "Fuel Vehicle" },
  "water cart": { color: "#06b6d4", label: "Water Cart" },
  "toilet cart": { color: "#8b5cf6", label: "Toilet Cart" },
  "ambulance": { color: "#ef4444", label: "Ambulance" },
  "push back tug": { color: "#64748b", label: "Push Back Tug" },
};

const TurnaroundGantt: React.FC = () => {
  const [events, setEvents] = useState<TurnaroundEvent[]>([]);
  const [selectedStand, setSelectedStand] = useState<string>('');
  const [stands, setStands] = useState<string[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await getTurnaroundEvents() as TurnaroundEvent[];
        setEvents(data);
        
        // Extract unique stands
        const uniqueStands = Array.from(new Set<string>(data.map((e: TurnaroundEvent) => e.stand))).sort();
        setStands(uniqueStands);
        if (uniqueStands.length > 0 && !selectedStand) {
          setSelectedStand(uniqueStands[0]);
        }
      } catch (error) {
        console.error("Error fetching turnaround events:", error);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 5000); // Poll every 5s
    return () => clearInterval(interval);
  }, [selectedStand]);

  // Process events for the selected stand
  const processTasks = () => {
    if (!selectedStand) return [];

    const standEvents = events.filter(e => e.stand === selectedStand);
    // Sort by time
    standEvents.sort((a, b) => new Date(a.eventTimeStamp).getTime() - new Date(b.eventTimeStamp).getTime());

    const tasks: GanttTask[] = [];
    const activeStarts: { [activity: string]: Date } = {};

    standEvents.forEach(event => {
      const normalizedActivity = normalizeActivity(event.activityType);
      const config = ACTIVITY_CONFIG[normalizedActivity];
      const label = config ? config.label : event.activityType;
      const color = config ? config.color : '#9ca3af';

      if (event.eventType === 0) { // START
        activeStarts[normalizedActivity] = new Date(event.eventTimeStamp);
      } else if (event.eventType === 1) { // STOP
        const startTime = activeStarts[normalizedActivity];
        if (startTime) {
          const endTime = new Date(event.eventTimeStamp);
          const duration = (endTime.getTime() - startTime.getTime()) / (1000 * 60); // minutes
          
          tasks.push({
            activity: label,
            start: startTime,
            end: endTime,
            duration: duration,
            color: color
          });
          
          delete activeStarts[normalizedActivity];
        }
      }
    });

    // Handle ongoing tasks
    Object.keys(activeStarts).forEach(activityKey => {
      const startTime = activeStarts[activityKey];
      const endTime = new Date(); // Current time for ongoing
      const duration = (endTime.getTime() - startTime.getTime()) / (1000 * 60);
      const config = ACTIVITY_CONFIG[activityKey];
      
      tasks.push({
        activity: config ? config.label : activityKey,
        start: startTime,
        end: endTime,
        duration: duration,
        color: config ? config.color : '#9ca3af'
      });
    });

    return tasks;
  };

  const tasks = processTasks();

  // Calculate timeline bounds
  const minTime = tasks.length > 0 ? new Date(Math.min(...tasks.map(t => t.start.getTime()))) : new Date();
  const maxTime = tasks.length > 0 ? new Date(Math.max(...tasks.map(t => (t.end || new Date()).getTime()))) : new Date();
  
  // Add buffer
  minTime.setMinutes(minTime.getMinutes() - 5);
  maxTime.setMinutes(maxTime.getMinutes() + 5);

  const totalDuration = (maxTime.getTime() - minTime.getTime());

  const getLeftPos = (date: Date) => {
    return ((date.getTime() - minTime.getTime()) / totalDuration) * 100;
  };

  const getWidth = (start: Date, end?: Date) => {
    const endTime = end || new Date();
    return ((endTime.getTime() - start.getTime()) / totalDuration) * 100;
  };

  return (
    <div className="p-4 bg-white rounded-lg shadow">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold">Turnaround Process (Gantt View)</h2>
        <select 
          value={selectedStand} 
          onChange={(e) => setSelectedStand(e.target.value)}
          className="p-2 border rounded"
          aria-label="Select Stand"
        >
          {stands.map(s => <option key={s} value={s}>Stand {s}</option>)}
        </select>
      </div>

      <div className="overflow-x-auto">
        <div className="min-w-[800px]">
          {/* Header Timeline */}
          <div className="flex border-b pb-2 mb-2">
            <div className="w-1/4 font-semibold text-gray-600">Activity</div>
            <div className="w-3/4 relative h-6 text-xs text-gray-500">
              <span className="absolute left-0">{minTime.toLocaleTimeString()}</span>
              <span className="absolute right-0">{maxTime.toLocaleTimeString()}</span>
            </div>
          </div>

          {/* Rows */}
          {Object.values(ACTIVITY_CONFIG).map(config => {
            const activityLabel = config.label;
            // Find tasks for this activity
            const activityTasks = tasks.filter(t => t.activity === activityLabel);
            
            return (
              <div key={activityLabel} className="flex items-center border-b py-2 hover:bg-gray-50 h-10">
                <div className="w-1/4 text-sm truncate pr-2" title={activityLabel}>{activityLabel}</div>
                <div className="w-3/4 relative h-6 bg-gray-100 rounded">
                  {activityTasks.map((task, idx) => (
                    <div
                      key={idx}
                      className="absolute h-4 top-1 rounded text-[10px] text-white flex items-center justify-center overflow-hidden whitespace-nowrap px-1"
                      style={{
                        left: `${getLeftPos(task.start)}%`,
                        width: `${getWidth(task.start, task.end)}%`,
                        backgroundColor: task.color
                      }}
                      title={`${task.activity}: ${task.start.toLocaleTimeString()} - ${task.end?.toLocaleTimeString()}`}
                    >
                      {task.duration?.toFixed(1)}m
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default TurnaroundGantt;
