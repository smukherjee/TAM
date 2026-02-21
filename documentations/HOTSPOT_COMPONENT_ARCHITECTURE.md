# Hotspot Analysis Feature - Component Architecture
**Feature**: 005-asset-tracking-security (Tasks T051-T059)  
**Status**: ✅ COMPLETE

---

## Component Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                         App.tsx                                  │
│                    (Route Configuration)                         │
│                                                                   │
│  Route: /tracking/hotspots                                       │
│    ↓ (ProtectedRoute)                                           │
│    ↓                                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              MainLayout                                  │   │
│  │        (Navigation + Content Area)                       │   │
│  │                                                           │   │
│  │  Navigation Menu:                                        │   │
│  │  └─ Airside Operations Section                          │   │
│  │      ├─ Live Asset Map (/tracking/assets)              │   │
│  │      └─ Hotspot Analysis (/tracking/hotspots) ◄─────   │   │
│  │                                                   Active  │   │
│  │  Content Area:                                           │   │
│  │  ┌───────────────────────────────────────────────────┐  │   │
│  │  │      HotspotAnalysisPage.tsx                      │  │   │
│  │  │      ┌───────────────────────────────────────┐    │  │   │
│  │  │      │  Filter Controls                      │    │  │   │
│  │  │      │  - Mode selector (Activity/Violations│    │  │   │
│  │  │      │    /Dwell)                           │    │  │   │
│  │  │      │  - Time range picker                 │    │  │   │
│  │  │      │  - Grid size slider                  │    │  │   │
│  │  │      │  - Export dropdown (PNG/CSV/PDF)     │    │  │   │
│  │  │      └───────────────────────────────────────┘    │  │   │
│  │  │                                                    │  │   │
│  │  │      ┌───────────────────────────────────────┐    │  │   │
│  │  │      │  Leaflet Map                         │    │  │   │
│  │  │      │  ┌─────────────────────────────┐     │    │  │   │
│  │  │      │  │  react-leaflet-heat         │     │    │  │   │
│  │  │      │  │  - Heatmap overlay           │     │    │  │   │
│  │  │      │  │  - Intensity gradient        │     │    │  │   │
│  │  │      │  │  - Click handlers            │     │    │  │   │
│  │  │      │  └─────────────────────────────┘     │    │  │   │
│  │  │      │                                       │    │  │   │
│  │  │      │  ┌─────────────────────────────┐     │    │  │   │
│  │  │      │  │  HeatmapLegend.tsx          │     │    │  │   │
│  │  │      │  │  - Color gradient bar        │     │    │  │   │
│  │  │      │  │  - Intensity labels          │     │    │  │   │
│  │  │      │  │  - Mode indicator            │     │    │  │   │
│  │  │      │  └─────────────────────────────┘     │    │  │   │
│  │  │      └───────────────────────────────────────┘    │  │   │
│  │  │                                                    │  │   │
│  │  │      ┌───────────────────────────────────────┐    │  │   │
│  │  │      │  HotspotDetailModal.tsx              │    │  │   │
│  │  │      │  (Triggered on cell click)            │    │  │   │
│  │  │      │                                       │    │  │   │
│  │  │      │  ┌─────────────────────────────┐     │    │  │   │
│  │  │      │  │  Headless UI Dialog         │     │    │  │   │
│  │  │      │  │  ┌─────────────────────┐    │     │    │  │   │
│  │  │      │  │  │ Header              │    │     │    │  │   │
│  │  │      │  │  │ - Title             │    │     │    │  │   │
│  │  │      │  │  │ - Close button      │    │     │    │  │   │
│  │  │      │  │  └─────────────────────┘    │     │    │  │   │
│  │  │      │  │                              │     │    │  │   │
│  │  │      │  │  ┌─────────────────────┐    │     │    │  │   │
│  │  │      │  │  │ Location Display    │    │     │    │  │   │
│  │  │      │  │  │ - Lat/Lng           │    │     │    │  │   │
│  │  │      │  │  │ - Copy button       │    │     │    │  │   │
│  │  │      │  │  └─────────────────────┘    │     │    │  │   │
│  │  │      │  │                              │     │    │  │   │
│  │  │      │  │  ┌─────────────────────┐    │     │    │  │   │
│  │  │      │  │  │ Statistics Cards    │    │     │    │  │   │
│  │  │      │  │  │ - Activity mode:    │    │     │    │  │   │
│  │  │      │  │  │   * Total movements │    │     │    │  │   │
│  │  │      │  │  │   * Unique assets   │    │     │    │  │   │
│  │  │      │  │  │   * Avg speed       │    │     │    │  │   │
│  │  │      │  │  │ - Violation mode:   │    │     │    │  │   │
│  │  │      │  │  │   * Severity counts │    │     │    │  │   │
│  │  │      │  │  │ - Dwell mode:       │    │     │    │  │   │
│  │  │      │  │  │   * Dwell times     │    │     │    │  │   │
│  │  │      │  │  └─────────────────────┘    │     │    │  │   │
│  │  │      │  │                              │     │    │  │   │
│  │  │      │  │  ┌─────────────────────┐    │     │    │  │   │
│  │  │      │  │  │ Asset List Table    │    │     │    │  │   │
│  │  │      │  │  │ - ID, Name, Category│    │     │    │  │   │
│  │  │      │  │  │ - Scrollable        │    │     │    │  │   │
│  │  │      │  │  └─────────────────────┘    │     │    │  │   │
│  │  │      │  │                              │     │    │  │   │
│  │  │      │  │  ┌─────────────────────┐    │     │    │  │   │
│  │  │      │  │  │ Recharts BarChart   │    │     │    │  │   │
│  │  │      │  │  │ - Hourly distribution│    │     │    │  │   │
│  │  │      │  │  │ - 24 hour timeline  │    │     │    │  │   │
│  │  │      │  │  └─────────────────────┘    │     │    │  │   │
│  │  │      │  │                              │     │    │  │   │
│  │  │      │  │  ┌─────────────────────┐    │     │    │  │   │
│  │  │      │  │  │ Navigation Buttons  │    │     │    │  │   │
│  │  │      │  │  │ - View Assets ──────┼────┼─────┼────┼───┐
│  │  │      │  │  │ - View Violations ──┼────┼─────┼────┼───┼─┐
│  │  │      │  │  └─────────────────────┘    │     │    │  │   │ │
│  │  │      │  └──────────────────────────────┘     │    │  │   │ │
│  │  │      └───────────────────────────────────────┘    │  │   │ │
│  │  └──────────────────────────────────────────────────┘  │   │ │
│  └─────────────────────────────────────────────────────────┘   │ │
└─────────────────────────────────────────────────────────────────┘ │
                                                                     │ │
    ┌────────────────────────────────────────────────────────────────┘ │
    │ Navigate to: /tracking/assets?lat={lat}&lng={lng}&radius={r}    │
    │                                                                   │
    │ ┌─────────────────────────────────────────────────────────────┐  │
    │ │           Live Asset Map Page                                │  │
    │ │           (Filtered by location)                             │  │
    │ └─────────────────────────────────────────────────────────────┘  │
    │                                                                   │
    └───────────────────────────────────────────────────────────────────┘
       Navigate to: /reports/violations?lat={lat}&lng={lng}&...
       
       ┌─────────────────────────────────────────────────────────────┐
       │           Violation Report Page                              │
       │           (Filtered by location + time)                      │
       └─────────────────────────────────────────────────────────────┘
```

---

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     User Interactions                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              HotspotAnalysisPage State                           │
│  - mode: 'activity' | 'violations' | 'dwell'                    │
│  - gridSize: 10 | 25 | 50 | 100                                 │
│  - timeRange: { start: Date, end: Date }                        │
│  - modalOpen: boolean                                            │
│  - selectedLocation: { lat: number, lng: number }               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   React Query Hooks                              │
│  - useQuery(['heatmap-data', mode, timeRange, gridSize])        │
│  - Fetches from heatmapService.ts                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   HeatmapService.ts                              │
│  - fetchActivityHeatmap(filters)                                 │
│  - fetchViolationHeatmap(filters)                                │
│  - fetchDwellHeatmap(filters)                                    │
│  - fetchHotspotDetail(lat, lng, mode, ...)                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   API Endpoints                                  │
│  GET /api/tracking/heatmap/activity?gridSize=10&...             │
│  GET /api/tracking/heatmap/violations?gridSize=10&...           │
│  GET /api/tracking/heatmap/dwell?gridSize=10&...                │
│  GET /api/tracking/heatmap/hotspot?lat=-27.3842&lng=153.1175&...│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Backend Services                               │
│  - HeatmapService.java                                           │
│  - AssetLocationService.java                                     │
│  - ViolationService.java                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Database (PostgreSQL)                          │
│  - asset_location (time-series data)                            │
│  - asset_metadata                                                │
│  - violation_records                                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Export Flow

```
User clicks "Export" → Selects format (PNG/CSV/PDF)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   handleExport()                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
                ▼             ▼             ▼
         ┌──────────┐  ┌──────────┐  ┌──────────┐
         │   PNG    │  │   CSV    │  │   PDF    │
         └──────────┘  └──────────┘  └──────────┘
                │             │             │
                ▼             ▼             ▼
    ┌────────────────┐ ┌────────────┐ ┌────────────┐
    │ html2canvas    │ │ Papa Parse │ │ jsPDF      │
    │ - Capture map  │ │ - Format   │ │ - Generate │
    │ - Save PNG     │ │   data     │ │   report   │
    └────────────────┘ │ - Save CSV │ │ - Add      │
                       └────────────┘ │   metadata │
                                      │ - Add table│
                                      │ - Save PDF │
                                      └────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Download File    │
                    │ heatmap_{mode}_  │
                    │ {date}.{ext}     │
                    └──────────────────┘
```

---

## TypeScript Type System

```
┌─────────────────────────────────────────────────────────────────┐
│              frontend/src/types/heatmap.ts                       │
│                                                                   │
│  interface HeatmapPoint {                                        │
│      latitude: number;                                           │
│      longitude: number;                                          │
│      intensity: number;                                          │
│      count: number;                                              │
│      metadata?: { assetIds?, violationIds?, zoneId? };          │
│  }                                                                │
│                                                                   │
│  interface HotspotDetail {                                       │
│      location: { latitude, longitude };                         │
│      mode: HeatmapMode;                                          │
│      gridSize: number;                                           │
│      activityCount?: number;                                     │
│      assets?: HotspotAsset[];                                    │
│      violations?: HotspotViolation[];                            │
│      dwellStats?: DwellStatistics;                               │
│      timeDistribution?: TimeDistributionPoint[];                 │
│  }                                                                │
│                                                                   │
│  interface HeatmapFilters {                                      │
│      tenantCode: string;                                         │
│      startDate: Date;                                            │
│      endDate: Date;                                              │
│      gridSize: number;                                           │
│      zoneId?: string;                                            │
│      category?: string;                                          │
│  }                                                                │
│                                                                   │
│  enum HeatmapMode {                                              │
│      ACTIVITY = 'activity',                                      │
│      VIOLATION = 'violations',                                   │
│      DWELL = 'dwell'                                             │
│  }                                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ (imported by)
                              │
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ HotspotAnalysis  │ │ HotspotDetail    │ │ heatmapService   │
│ Page.tsx         │ │ Modal.tsx        │ │ .ts              │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

---

## Dependencies Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                     package.json                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         React Ecosystem                 │
        │  - react: ^19.0.0                       │
        │  - react-dom: ^19.0.0                   │
        │  - react-router-dom: ^7.1.1             │
        │  - @tanstack/react-query: ^5.62.18      │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         UI Components                    │
        │  - @headlessui/react: ^1.7.18 (NEW)     │
        │  - lucide-react: ^0.468.0                │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         Mapping Libraries                │
        │  - leaflet: ^1.9.4                       │
        │  - react-leaflet: ^4.2.1                 │
        │  - leaflet.heat: ^0.2.0                  │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         Charts & Visualization           │
        │  - recharts: ^2.12.0 (NEW)               │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         Export Libraries                 │
        │  - html2canvas: ^1.4.1 (NEW)             │
        │  - jspdf: ^2.5.1 (NEW)                   │
        │  - papaparse: ^5.4.1                     │
        └─────────────────────────────────────────┘
```

---

## Task Completion Status

```
T051: HotspotDetailModal ────────────────────── ✅ COMPLETE
  │
  ├─ Modal framework (Headless UI) ──────────── ✅
  ├─ Data fetching (React Query) ───────────── ✅
  ├─ Mode-specific statistics ──────────────── ✅
  ├─ Asset list table ──────────────────────── ✅
  ├─ Time distribution chart ───────────────── ✅
  ├─ Location display + copy ───────────────── ✅
  └─ Navigation buttons ────────────────────── ✅

T054: Export Functionality ──────────────────── ✅ COMPLETE
  │
  ├─ PNG export (html2canvas) ──────────────── ✅
  ├─ CSV export (Papa Parse) ───────────────── ✅
  └─ PDF export (jsPDF) ────────────────────── ✅

T056: TypeScript Interfaces ─────────────────── ✅ COMPLETE
  │
  ├─ HeatmapPoint interface ────────────────── ✅
  ├─ HotspotDetail interface ───────────────── ✅
  ├─ HeatmapFilters interface ──────────────── ✅
  ├─ HeatmapMode enum ──────────────────────── ✅
  └─ HeatmapStatistics interface ───────────── ✅

T057: MainLayout Navigation ─────────────────── ✅ COMPLETE
  │
  ├─ "Airside Operations" section ──────────── ✅
  ├─ "Live Asset Map" menu item ────────────── ✅
  ├─ "Hotspot Analysis" menu item ──────────── ✅
  └─ Role-based visibility ─────────────────── ✅

T058: Route Protection ──────────────────────── ✅ COMPLETE
  │
  ├─ /tracking/assets route ────────────────── ✅
  ├─ /tracking/hotspots route ──────────────── ✅
  └─ ProtectedRoute wrapper ────────────────── ✅

T059: Cross-Linking ─────────────────────────── ✅ COMPLETE
  │
  ├─ Hotspot → Asset map ───────────────────── ✅
  ├─ Hotspot → Violation report ────────────── ✅
  └─ URL param preservation ────────────────── ✅
```

---

**Generated**: January 24, 2025  
**All Tasks**: ✅ COMPLETE (9/9)  
**Status**: 🟢 READY FOR TESTING
