package com.onthehill.xaeroworldmapbook.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkVisitLedgerTest
{
    @Test
    void recordVisit_neverSeenChunkKey_returnsTrueAndIncreasesCount()
    {
        // Arrange
        ChunkVisitLedger ledger = new ChunkVisitLedger();

        // Act
        boolean result = ledger.recordVisit(42L);

        // Assert
        assertTrue(result);
        assertEquals(1, ledger.size());
    }

    @Test
    void recordVisit_alreadyVisitedChunkKey_returnsFalseAndCountUnchanged()
    {
        // Arrange
        ChunkVisitLedger ledger = new ChunkVisitLedger();
        ledger.recordVisit(42L);

        // Act
        boolean result = ledger.recordVisit(42L);

        // Assert
        assertFalse(result);
        assertEquals(1, ledger.size());
    }

    @Test
    void recordVisit_zeroChunksVisitedYet_startsAtCountZero()
    {
        // Arrange
        ChunkVisitLedger ledger = new ChunkVisitLedger();

        // Act
        int size = ledger.size();

        // Assert
        assertEquals(0, size);
    }

    @Test
    void recordVisit_manyDistinctKeysAcrossSimulatedDimensions_countsEachExactlyOnce()
    {
        // Arrange
        ChunkVisitLedger ledger = new ChunkVisitLedger();
        // Simulates two "dimensions" whose raw chunk-position packing would
        // collide under a naive same-dimension-only scheme: dimension 0's
        // chunk (0, 0) and dimension 1's chunk (0, 0), disambiguated here by
        // packing the dimension index into the high bits, the same shape
        // ChunkVisitTracker's real packing must produce.
        long dimensionZeroChunkZeroZero = 0L;
        long dimensionOneChunkZeroZero = 1L << 40;

        // Act
        boolean firstNew = ledger.recordVisit(dimensionZeroChunkZeroZero);
        boolean secondNew = ledger.recordVisit(dimensionOneChunkZeroZero);

        // Assert
        assertTrue(firstNew);
        assertTrue(secondNew);
        assertEquals(2, ledger.size());
    }
}
