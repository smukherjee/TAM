package com.utam.simulation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.entity.AirportBoundary;
import com.utam.repository.AirportBoundaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoundaryValidator.
 */
@ExtendWith(MockitoExtension.class)
class BoundaryValidatorTest {

    @Mock
    private AirportBoundaryRepository boundaryRepository;

    private BoundaryValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new BoundaryValidator(boundaryRepository, objectMapper);
    }

    @Test
    @DisplayName("Should validate point inside polygon")
    void shouldValidatePointInsidePolygon() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[10.0,0.0],[10.0,10.0],[0.0,10.0],[0.0,0.0]]]}";
        
        AirportBoundary boundary = createBoundary("TEST", geoJson, 0.0, 10.0, 0.0, 10.0);
        when(boundaryRepository.findByTenantCode("TEST")).thenReturn(Optional.of(boundary));

        assertTrue(validator.isWithinBoundary("TEST", 5.0, 5.0));
        assertTrue(validator.isWithinBoundary("TEST", 1.0, 1.0));
        assertTrue(validator.isWithinBoundary("TEST", 9.0, 9.0));
    }

    @Test
    @DisplayName("Should validate point outside polygon")
    void shouldValidatePointOutsidePolygon() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[10.0,0.0],[10.0,10.0],[0.0,10.0],[0.0,0.0]]]}";
        
        AirportBoundary boundary = createBoundary("TEST", geoJson, 0.0, 10.0, 0.0, 10.0);
        when(boundaryRepository.findByTenantCode("TEST")).thenReturn(Optional.of(boundary));

        assertFalse(validator.isWithinBoundary("TEST", -1.0, 5.0));
        assertFalse(validator.isWithinBoundary("TEST", 11.0, 5.0));
        assertFalse(validator.isWithinBoundary("TEST", 5.0, -1.0));
        assertFalse(validator.isWithinBoundary("TEST", 5.0, 11.0));
    }

    @Test
    @DisplayName("Should allow all positions when no boundary defined")
    void shouldAllowAllPositionsWhenNoBoundaryDefined() {
        when(boundaryRepository.findByTenantCode("UNKNOWN")).thenReturn(Optional.empty());

        assertTrue(validator.isWithinBoundary("UNKNOWN", 0.0, 0.0));
        assertTrue(validator.isWithinBoundary("UNKNOWN", 100.0, 100.0));
    }

    @Test
    @DisplayName("Should handle real airport coordinates")
    void shouldHandleRealAirportCoordinates() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[77.080,28.550],[77.130,28.550],[77.130,28.585],[77.080,28.585],[77.080,28.550]]]}";
        
        AirportBoundary boundary = createBoundary("VIDP", geoJson, 28.550, 28.585, 77.080, 77.130);
        when(boundaryRepository.findByTenantCode("VIDP")).thenReturn(Optional.of(boundary));

        assertTrue(validator.isWithinBoundary("VIDP", 28.5665, 77.1031));
        assertFalse(validator.isWithinBoundary("VIDP", 28.4, 77.0));
    }

    @Test
    @DisplayName("Should clamp position to boundary")
    void shouldClampPositionToBoundary() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[10.0,0.0],[10.0,10.0],[0.0,10.0],[0.0,0.0]]]}";
        
        AirportBoundary boundary = createBoundary("TEST", geoJson, 0.0, 10.0, 0.0, 10.0);
        when(boundaryRepository.findByTenantCode("TEST")).thenReturn(Optional.of(boundary));

        BoundaryValidator.ClampedPosition insideResult = validator.clampToBoundary("TEST", 5.0, 5.0);
        assertEquals(5.0, insideResult.latitude());
        assertEquals(5.0, insideResult.longitude());
        assertFalse(insideResult.wasClamped());

        BoundaryValidator.ClampedPosition outsideResult = validator.clampToBoundary("TEST", 15.0, 15.0);
        assertEquals(10.0, outsideResult.latitude());
        assertEquals(10.0, outsideResult.longitude());
        assertTrue(outsideResult.wasClamped());
    }

    @Test
    @DisplayName("Should return original position when no boundary defined for clamping")
    void shouldReturnOriginalPositionWhenNoBoundaryForClamping() {
        when(boundaryRepository.findByTenantCode("UNKNOWN")).thenReturn(Optional.empty());

        BoundaryValidator.ClampedPosition result = validator.clampToBoundary("UNKNOWN", 100.0, 200.0);
        assertEquals(100.0, result.latitude());
        assertEquals(200.0, result.longitude());
        assertFalse(result.wasClamped());
    }

    @Test
    @DisplayName("Should get boundary center")
    void shouldGetBoundaryCenter() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[10.0,0.0],[10.0,10.0],[0.0,10.0],[0.0,0.0]]]}";
        
        AirportBoundary boundary = createBoundary("TEST", geoJson, 0.0, 10.0, 0.0, 10.0);
        when(boundaryRepository.findByTenantCode("TEST")).thenReturn(Optional.of(boundary));

        Optional<double[]> center = validator.getBoundaryCenter("TEST");
        assertTrue(center.isPresent());
        assertEquals(5.0, center.get()[0], 0.001);
        assertEquals(5.0, center.get()[1], 0.001);
    }

    @Test
    @DisplayName("Should return empty for boundary center when no boundary defined")
    void shouldReturnEmptyForBoundaryCenterWhenNoBoundary() {
        when(boundaryRepository.findByTenantCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<double[]> center = validator.getBoundaryCenter("UNKNOWN");
        assertTrue(center.isEmpty());
    }

    private AirportBoundary createBoundary(String tenantCode, String geoJson, 
                                           Double minLat, Double maxLat, 
                                           Double minLon, Double maxLon) {
        AirportBoundary boundary = new AirportBoundary();
        boundary.setTenantCode(tenantCode);
        boundary.setBoundaryGeoJson(geoJson);
        boundary.setMinLatitude(minLat);
        boundary.setMaxLatitude(maxLat);
        boundary.setMinLongitude(minLon);
        boundary.setMaxLongitude(maxLon);
        return boundary;
    }
}
