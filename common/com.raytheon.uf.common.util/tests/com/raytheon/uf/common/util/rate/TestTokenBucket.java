/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.util.rate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for TokenBucket
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 23, 2016 5937       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class TestTokenBucket {

    @Test
    public void testOneArgConstructor() {
        TokenBucket bucket = new TokenBucket(10);
        assertEquals(bucket.getCapacity(), 10);
    }

    @Test
    public void testGetCapacity() {
        TokenBucket bucket = new TokenBucket(20, 1000);
        assertEquals(bucket.getCapacity(), 20);
    }

    @Test
    public void testConsumeReturnsNumberOfTokensConsumed() {
        int intervalMs = 100;
        int count = 10;
        int consumed = 0;
        TokenBucket bucket = new TokenBucket(count, intervalMs);
        try {
            consumed = bucket.consume(count);
        } catch (InterruptedException e) {
            // ignore
        }
        assertEquals(consumed, count);
    }

    @Test
    public void testConsumeBetweenReturnsNumberOfTokensConsumed() {
        int intervalMs = 100;
        int capacity = 10;
        int min = 3;
        int max = 5;
        int consumed = 0;
        TokenBucket bucket = new TokenBucket(capacity, intervalMs);
        try {
            consumed = bucket.consumeBetween(min, max);
        } catch (InterruptedException e) {
            // ignore
        }
        assertTrue("Consumed at least " + min + " tokens", consumed >= min);
        assertTrue("Consumed no more than " + max + " tokens", consumed <= max);
    }

    @Test
    public void testConsumeAfterWaitingFullIntervalDoesNotBlock() {
        int intervalMs = 100;
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, intervalMs);
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            // ignore
        }
        long t0 = System.currentTimeMillis();
        try {
            bucket.consume(count);
        } catch (InterruptedException e) {
            // ignore
        }
        long t1 = System.currentTimeMillis();
        // allow a few ms margin of error
        assertTrue("Did not block", t1 - t0 < 3);
    }

    @Test
    public void testConsumeImmediatelyBlocks() {
        int intervalMs = 100;
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, intervalMs);
        long t0 = System.currentTimeMillis();
        try {
            bucket.consume(count);
        } catch (InterruptedException e) {
            // ignore
        }
        long t1 = System.currentTimeMillis();
        // allow a few ms margin of error
        assertTrue("Consume took at least " + intervalMs + " ms",
                t1 - t0 >= intervalMs - 3);
    }

    @Test
    public void testConsumeAfterWaitingHalfIntervalBlocksForHalfInterval() {
        int intervalMs = 100;
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, intervalMs);
        try {
            Thread.sleep(intervalMs / 2);
        } catch (InterruptedException e) {
            // ignore
        }
        long t0 = System.currentTimeMillis();
        try {
            bucket.consume(count);
        } catch (InterruptedException e) {
            // ignore
        }
        long t1 = System.currentTimeMillis();
        // allow a few ms margin of error
        long diff = t1 - t0;
        assertTrue("Blocked for half an interval",
                diff < intervalMs / 2 + 3 && diff > intervalMs / 2 - 3);
    }

    @Test
    public void testConsumeOverCapacity() {
        int count = 10;
        int intervalMs = 20;
        TokenBucket bucket = new TokenBucket(count, intervalMs);
        long t0 = System.currentTimeMillis();
        int consumed = 0;
        try {
            consumed = bucket.consume(count * 2);
        } catch (InterruptedException e) {
            // ignore
        }
        long diff = System.currentTimeMillis() - t0;
        assertTrue("Blocked for at least one interval", diff > intervalMs);
        assertTrue("Consumed " + count * 2 + " tokens", consumed == count * 2);
    }

    @Test
    public void testConsumeNegativeThrowsException() {
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, 100);
        try {
            bucket.consume(-count);
            fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException | InterruptedException e) {
            // pass
        }
    }

    @Test
    public void testTryConsumeTooManyThrowsException() {
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, 100);
        try {
            bucket.tryConsume(count + 1);
            fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testTryConsumeNegativeThrowsException() {
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, 100);
        try {
            bucket.tryConsume(-count);
            fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testTryConsumeImmediatelyDoesNotBlock() {
        double intervalMs = 1000;
        int result = 0;
        int count = 10;
        TokenBucket bucket = new TokenBucket(count, (long) intervalMs);
        long t0 = System.currentTimeMillis();
        result = bucket.tryConsume(count);
        long t1 = System.currentTimeMillis();
        // allow a few ms margin of error
        assertTrue("tryConsume did not block", t1 - t0 < 3);
        assertFalse("tryConsume consumed tokens", result > 0);
    }

    @Test
    public void testTryConsumeHalfAfterWaitingHalfIntervalDoesNotBlock() {
        int intervalMs = 100;
        int count = 10;
        int result = 0;
        TokenBucket bucket = new TokenBucket(count, intervalMs);
        try {
            Thread.sleep(intervalMs / 2);
        } catch (InterruptedException e) {
            // ignore
        }
        long t0 = System.currentTimeMillis();
        result = bucket.tryConsume(count / 2);
        long t1 = System.currentTimeMillis();
        // allow a few ms margin of error
        assertTrue("tryConsume did not block", t1 - t0 < 3);
        assertTrue("tryConsume consumed tokens", result > 0);
    }
}
