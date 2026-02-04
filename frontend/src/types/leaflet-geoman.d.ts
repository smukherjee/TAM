/**
 * Type declarations for @geoman-io/leaflet-geoman-free
 * These extend the Leaflet types to include the PM (geoman) functionality
 */

import * as L from 'leaflet';

declare module 'leaflet' {
  interface Map {
    pm: PM.Map;
  }

  interface Layer {
    pm: PM.Layer;
  }

  interface Polygon {
    pm: PM.Layer;
  }

  interface Polyline {
    pm: PM.Layer;
  }

  interface Marker {
    pm: PM.Layer;
  }

  namespace PM {
    interface Map {
      addControls(options?: PMControlOptions): void;
      removeControls(): void;
      setGlobalOptions(options?: PMGlobalOptions): void;
      enableDraw(shape?: string, options?: PMDrawOptions): void;
      disableDraw(): void;
      toggleGlobalRemovalMode(): void;
      toggleGlobalDragMode(): void;
      toggleGlobalEditMode(): void;
      setPathOptions(options?: L.PathOptions): void;
    }

    interface Layer {
      enable(options?: PMEditOptions): void;
      disable(): void;
      enabled(): boolean;
      toggleEdit(options?: PMEditOptions): void;
      hasSelfIntersection(): boolean;
      remove(): void;
    }

    interface PMControlOptions {
      position?: L.ControlPosition;
      drawMarker?: boolean;
      drawCircleMarker?: boolean;
      drawPolyline?: boolean;
      drawRectangle?: boolean;
      drawPolygon?: boolean;
      drawCircle?: boolean;
      drawText?: boolean;
      editMode?: boolean;
      dragMode?: boolean;
      cutPolygon?: boolean;
      removalMode?: boolean;
      rotateMode?: boolean;
      snappingOption?: boolean;
      splitMode?: boolean;
      scaleMode?: boolean;
    }

    interface PMGlobalOptions {
      snappable?: boolean;
      snapDistance?: number;
      snapMiddle?: boolean;
      allowSelfIntersection?: boolean;
      templineStyle?: L.PathOptions;
      hintlineStyle?: L.PathOptions;
      pathOptions?: L.PathOptions;
      markerStyle?: L.MarkerOptions;
      finishOn?: string;
      tooltips?: boolean;
      continueDrawing?: boolean;
    }

    interface PMDrawOptions {
      snappable?: boolean;
      snapDistance?: number;
      pathOptions?: L.PathOptions;
      markerStyle?: L.MarkerOptions;
      templineStyle?: L.PathOptions;
      hintlineStyle?: L.PathOptions;
    }

    interface PMEditOptions {
      preventMarkerRemoval?: boolean;
      allowSelfIntersection?: boolean;
      draggable?: boolean;
      snappable?: boolean;
    }

    // Event handler types
    interface CreateEventHandler {
      shape: string;
      layer: L.Layer;
    }

    interface RemoveEventHandler {
      layer: L.Layer;
      shape: string;
    }

    interface EditEventHandler {
      layer: L.Layer;
      shape: string;
    }
  }
}

export {};
