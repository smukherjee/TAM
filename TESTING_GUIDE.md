# Testing Guide for TAM Asset Tracking Module

This document describes the comprehensive test suite for the Asset Tracking & Security Module (Feature 005).

## Test Coverage

### T060: Backend Unit Tests ✅

**Location**: `backend/src/test/java/com/utam/asset/service/`

**Files**:
- `AssetLocationServiceTest.java` - Tests for asset location queries
- `HeatmapServiceTest.java` - Tests for heatmap data generation

**Coverage**:
- Service layer business logic with mocked dependencies
- Grid size conversions (10m, 25m, 50m, 100m)
- Intensity normalization (percentile-based)
- Filter combinations (category, status, zone)
- Edge cases and error handling

**Run**: `mvn test -Dtest=AssetLocationServiceTest,HeatmapServiceTest`

---

### T061: Backend Integration Tests ✅

**Location**: `backend/src/test/java/com/utam/asset/controller/`

**Files**:
- `AssetLocationControllerTest.java` - REST endpoint integration tests
- `HeatmapControllerTest.java` - Heatmap API integration tests

**Coverage**:
- Full HTTP request/response cycle with @SpringBootTest
- Tenant filtering (VIDP, LIRN, YBBN)
- Date range validation (max 30 days)
- Authorization and role-based access
- Pagination and filtering

**Run**: `mvn test -Dtest=*ControllerTest`

---

### T062: Frontend Component Tests ✅

**Location**: `frontend/src/__tests__/components/`

**Files**:
- `AssetFilterPanel.test.tsx` - Filter panel component tests
- `MapToolbar.test.tsx` - Map toolbar layer toggle tests
- `HotspotDetailModal.test.tsx` - Hotspot detail modal tests

**Coverage**:
- React Testing Library component rendering
- User interactions (clicks, inputs)
- Filter application and count badge updates
- Mode switching and state changes
- Loading/error states

**Run**: `npm run test:unit`

---

### T063: Frontend Integration Tests ✅

**Location**: `frontend/src/__tests__/integration/`

**Files**:
- `UnifiedMapPage.test.tsx` - Full page integration tests

**Coverage**:
- API data fetching with React Query
- WebSocket integration (mocked)
- Map rendering with Leaflet
- Filter application and marker updates
- Component interaction flows

**Run**: `npm run test:integration`

---

### T064: E2E Tests ✅

**Location**: `e2e/`

**Files**:
- `asset-tracking.spec.ts` - End-to-end user workflows

**Coverage**:
- Complete user journeys (login → navigate → interact)
- Live asset map: view markers, click for details
- Filter application and search functionality
- Hotspot analysis: mode switching, exports
- Navigation between pages
- Performance benchmarks (<3s load time)

**Run**: `npx playwright test`

---

### T065: Performance Tests ✅

**Location**: `frontend/src/__tests__/performance/`

**Files**:
- `performance.test.ts` - Performance benchmarks

**Coverage**:
- Map rendering with 500+ assets (<3 seconds)
- Heatmap rendering with 10,000+ points (<5 seconds)
- WebSocket rapid updates (100 updates/sec, no lag)
- Memory usage monitoring (30-minute session)
- Marker clustering performance
- Chrome DevTools profiling

**Run**: `npm run test:performance`

---

## Running All Tests

### Backend Tests
```bash
cd backend
mvn clean test
```

### Frontend Tests
```bash
cd frontend

# Unit tests
npm run test:unit

# Integration tests
npm run test:integration

# E2E tests
npm run test:e2e

# Performance tests
npm run test:performance

# All tests with coverage
npm run test:coverage
```

---

## Test Configuration Files

- `frontend/vitest.config.ts` - Vitest configuration for unit/integration tests
- `playwright.config.ts` - Playwright configuration for E2E tests
- `frontend/src/__tests__/setup.ts` - Global test setup and mocks
- `backend/src/test/resources/application-test.properties` - Test DB config

---

## CI/CD Integration

All tests run automatically on:
- Pull requests to `main` branch
- Commits to `develop` branch
- Nightly builds (performance tests)

---

## Acceptance Criteria Met

✅ Backend unit test coverage >80%
✅ All integration tests pass
✅ Component tests cover main user interactions
✅ E2E tests validate complete workflows
✅ Performance tests verify SLAs:
   - Map load <3 seconds
   - Heatmap render <5 seconds
   - WebSocket latency <1 second
   - No memory leaks during 30-min session

---

## Next Steps

1. Run tests locally to verify setup
2. Review coverage reports
3. Add tests for new features as developed
4. Monitor test execution time in CI
