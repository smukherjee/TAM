/**
 * GeometryValidator
 * Utility for validating polygon geometry.
 * Implements FR-046: Polygon validation for zone editor.
 */

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface Point {
  lng: number;
  lat: number;
}

/**
 * Validate a polygon geometry.
 */
export function validatePolygon(coordinates: number[][]): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  // Check minimum points
  if (!coordinates || coordinates.length < 3) {
    errors.push('Polygon must have at least 3 points');
    return { valid: false, errors, warnings };
  }

  // Check coordinate format
  for (let i = 0; i < coordinates.length; i++) {
    const coord = coordinates[i];
    if (!Array.isArray(coord) || coord.length < 2) {
      errors.push(`Invalid coordinate at index ${i}: must be [lng, lat]`);
      continue;
    }

    const [lng, lat] = coord;

    // Validate longitude
    if (typeof lng !== 'number' || isNaN(lng)) {
      errors.push(`Invalid longitude at point ${i + 1}`);
    } else if (lng < -180 || lng > 180) {
      errors.push(`Longitude out of range at point ${i + 1}: ${lng}`);
    }

    // Validate latitude
    if (typeof lat !== 'number' || isNaN(lat)) {
      errors.push(`Invalid latitude at point ${i + 1}`);
    } else if (lat < -90 || lat > 90) {
      errors.push(`Latitude out of range at point ${i + 1}: ${lat}`);
    }
  }

  if (errors.length > 0) {
    return { valid: false, errors, warnings };
  }

  // Check for self-intersection
  if (hasSelfIntersection(coordinates)) {
    errors.push('Polygon has self-intersecting edges');
  }

  // Check for duplicate consecutive points
  const duplicates = findDuplicatePoints(coordinates);
  if (duplicates.length > 0) {
    warnings.push(`Duplicate points at indices: ${duplicates.join(', ')}`);
  }

  // Check for very small area
  const area = calculateArea(coordinates);
  if (area < 100) {
    // Less than 100 square meters
    warnings.push('Polygon area is very small (< 100 m²)');
  }

  // Check for clockwise/counter-clockwise
  if (!isCounterClockwise(coordinates)) {
    warnings.push('Polygon is clockwise (should be counter-clockwise for GeoJSON)');
  }

  // Check for very acute angles
  const acuteAngles = findAcuteAngles(coordinates);
  if (acuteAngles.length > 0) {
    warnings.push(`Very acute angles at points: ${acuteAngles.join(', ')}`);
  }

  return {
    valid: errors.length === 0,
    errors,
    warnings,
  };
}

/**
 * Check if polygon has self-intersecting edges.
 */
function hasSelfIntersection(coordinates: number[][]): boolean {
  const n = coordinates.length;

  for (let i = 0; i < n; i++) {
    const a1 = coordinates[i];
    const a2 = coordinates[(i + 1) % n];

    // Check against non-adjacent edges
    for (let j = i + 2; j < n; j++) {
      // Skip adjacent edges
      if (j === (i + n - 1) % n) continue;

      const b1 = coordinates[j];
      const b2 = coordinates[(j + 1) % n];

      if (segmentsIntersect(a1, a2, b1, b2)) {
        return true;
      }
    }
  }

  return false;
}

/**
 * Check if two line segments intersect.
 */
function segmentsIntersect(
  a1: number[],
  a2: number[],
  b1: number[],
  b2: number[]
): boolean {
  const d1 = direction(b1, b2, a1);
  const d2 = direction(b1, b2, a2);
  const d3 = direction(a1, a2, b1);
  const d4 = direction(a1, a2, b2);

  if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
      ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
    return true;
  }

  if (d1 === 0 && onSegment(b1, b2, a1)) return true;
  if (d2 === 0 && onSegment(b1, b2, a2)) return true;
  if (d3 === 0 && onSegment(a1, a2, b1)) return true;
  if (d4 === 0 && onSegment(a1, a2, b2)) return true;

  return false;
}

/**
 * Calculate cross product direction.
 */
function direction(p1: number[], p2: number[], p3: number[]): number {
  return (p3[0] - p1[0]) * (p2[1] - p1[1]) - (p2[0] - p1[0]) * (p3[1] - p1[1]);
}

/**
 * Check if point is on segment.
 */
function onSegment(p1: number[], p2: number[], p: number[]): boolean {
  return (
    p[0] <= Math.max(p1[0], p2[0]) &&
    p[0] >= Math.min(p1[0], p2[0]) &&
    p[1] <= Math.max(p1[1], p2[1]) &&
    p[1] >= Math.min(p1[1], p2[1])
  );
}

/**
 * Find indices of duplicate consecutive points.
 */
function findDuplicatePoints(coordinates: number[][]): number[] {
  const duplicates: number[] = [];
  const n = coordinates.length;

  for (let i = 0; i < n; i++) {
    const current = coordinates[i];
    const next = coordinates[(i + 1) % n];

    if (current[0] === next[0] && current[1] === next[1]) {
      duplicates.push(i + 1);
    }
  }

  return duplicates;
}

/**
 * Check if polygon is counter-clockwise.
 */
function isCounterClockwise(coordinates: number[][]): boolean {
  let sum = 0;
  const n = coordinates.length;

  for (let i = 0; i < n; i++) {
    const current = coordinates[i];
    const next = coordinates[(i + 1) % n];
    sum += (next[0] - current[0]) * (next[1] + current[1]);
  }

  return sum < 0;
}

/**
 * Calculate polygon area in square meters (approximate).
 */
function calculateArea(coordinates: number[][]): number {
  if (coordinates.length < 3) return 0;

  let area = 0;
  const n = coordinates.length;

  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n;
    const [lng1, lat1] = coordinates[i];
    const [lng2, lat2] = coordinates[j];

    // Approximate conversion at this latitude
    const avgLat = ((lat1 + lat2) / 2) * (Math.PI / 180);
    const metersPerDegreeLat = 111320;
    const metersPerDegreeLng = 111320 * Math.cos(avgLat);

    const x1 = lng1 * metersPerDegreeLng;
    const y1 = lat1 * metersPerDegreeLat;
    const x2 = lng2 * metersPerDegreeLng;
    const y2 = lat2 * metersPerDegreeLat;

    area += x1 * y2 - x2 * y1;
  }

  return Math.abs(area) / 2;
}

/**
 * Find points with very acute angles (< 15 degrees).
 */
function findAcuteAngles(coordinates: number[][]): number[] {
  const acutePoints: number[] = [];
  const n = coordinates.length;
  const threshold = 15; // degrees

  for (let i = 0; i < n; i++) {
    const prev = coordinates[(i - 1 + n) % n];
    const current = coordinates[i];
    const next = coordinates[(i + 1) % n];

    const angle = calculateAngle(prev, current, next);

    if (angle < threshold) {
      acutePoints.push(i + 1);
    }
  }

  return acutePoints;
}

/**
 * Calculate angle at a point in degrees.
 */
function calculateAngle(p1: number[], p2: number[], p3: number[]): number {
  const v1 = [p1[0] - p2[0], p1[1] - p2[1]];
  const v2 = [p3[0] - p2[0], p3[1] - p2[1]];

  const dot = v1[0] * v2[0] + v1[1] * v2[1];
  const mag1 = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
  const mag2 = Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);

  if (mag1 === 0 || mag2 === 0) return 0;

  const cosAngle = Math.max(-1, Math.min(1, dot / (mag1 * mag2)));
  return Math.acos(cosAngle) * (180 / Math.PI);
}

/**
 * Simplify polygon using Douglas-Peucker algorithm.
 */
export function simplifyPolygon(coordinates: number[][], tolerance: number = 0.0001): number[][] {
  if (coordinates.length < 3) return coordinates;

  const simplified = douglasPeucker(coordinates, tolerance);

  // Ensure at least 3 points
  if (simplified.length < 3) {
    return coordinates;
  }

  return simplified;
}

/**
 * Douglas-Peucker line simplification.
 */
function douglasPeucker(points: number[][], tolerance: number): number[][] {
  if (points.length < 3) return points;

  // Find point with maximum distance
  let maxDist = 0;
  let maxIndex = 0;

  const first = points[0];
  const last = points[points.length - 1];

  for (let i = 1; i < points.length - 1; i++) {
    const dist = perpendicularDistance(points[i], first, last);
    if (dist > maxDist) {
      maxDist = dist;
      maxIndex = i;
    }
  }

  // If max distance > tolerance, recursively simplify
  if (maxDist > tolerance) {
    const left = douglasPeucker(points.slice(0, maxIndex + 1), tolerance);
    const right = douglasPeucker(points.slice(maxIndex), tolerance);

    return [...left.slice(0, -1), ...right];
  }

  // Otherwise, return just endpoints
  return [first, last];
}

/**
 * Calculate perpendicular distance from point to line.
 */
function perpendicularDistance(point: number[], lineStart: number[], lineEnd: number[]): number {
  const dx = lineEnd[0] - lineStart[0];
  const dy = lineEnd[1] - lineStart[1];

  if (dx === 0 && dy === 0) {
    // Line is a point
    return Math.sqrt(
      Math.pow(point[0] - lineStart[0], 2) + Math.pow(point[1] - lineStart[1], 2)
    );
  }

  const t = ((point[0] - lineStart[0]) * dx + (point[1] - lineStart[1]) * dy) / (dx * dx + dy * dy);

  const closestX = lineStart[0] + t * dx;
  const closestY = lineStart[1] + t * dy;

  return Math.sqrt(Math.pow(point[0] - closestX, 2) + Math.pow(point[1] - closestY, 2));
}

/**
 * Close polygon if not already closed.
 */
export function closePolygon(coordinates: number[][]): number[][] {
  if (coordinates.length < 3) return coordinates;

  const first = coordinates[0];
  const last = coordinates[coordinates.length - 1];

  if (first[0] !== last[0] || first[1] !== last[1]) {
    return [...coordinates, first];
  }

  return coordinates;
}

/**
 * Ensure polygon is counter-clockwise (GeoJSON standard).
 */
export function ensureCounterClockwise(coordinates: number[][]): number[][] {
  if (isCounterClockwise(coordinates)) {
    return coordinates;
  }
  return [...coordinates].reverse();
}

/**
 * Calculate centroid of polygon.
 */
export function calculateCentroid(coordinates: number[][]): Point {
  if (coordinates.length === 0) {
    return { lng: 0, lat: 0 };
  }

  let sumLng = 0;
  let sumLat = 0;

  for (const coord of coordinates) {
    sumLng += coord[0];
    sumLat += coord[1];
  }

  return {
    lng: sumLng / coordinates.length,
    lat: sumLat / coordinates.length,
  };
}

/**
 * Calculate bounding box of polygon.
 */
export function calculateBoundingBox(coordinates: number[][]): {
  minLng: number;
  maxLng: number;
  minLat: number;
  maxLat: number;
} {
  if (coordinates.length === 0) {
    return { minLng: 0, maxLng: 0, minLat: 0, maxLat: 0 };
  }

  let minLng = Infinity;
  let maxLng = -Infinity;
  let minLat = Infinity;
  let maxLat = -Infinity;

  for (const coord of coordinates) {
    minLng = Math.min(minLng, coord[0]);
    maxLng = Math.max(maxLng, coord[0]);
    minLat = Math.min(minLat, coord[1]);
    maxLat = Math.max(maxLat, coord[1]);
  }

  return { minLng, maxLng, minLat, maxLat };
}

export default {
  validatePolygon,
  simplifyPolygon,
  closePolygon,
  ensureCounterClockwise,
  calculateCentroid,
  calculateBoundingBox,
};
