package com.styra.opa.wasm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MemoryTest {
    static Path wasmFile;

    @BeforeAll
    public static void beforeAll() throws Exception {
        wasmFile = OpaCli.compile("memory", "test/allow").resolve("policy.wasm");
    }

    @Test
    public void inputExceedsMemoryHostFailsToGrowIt() {
        var policy =
                Opa.loadPolicy(
                        wasmFile,
                        OpaDefaultImports.builder().withMemoryInitial(2).withMemoryMax(2).build());
        var input = new String(new char[2 * 65536]).replace("\0", "a");
        var exception = assertThrows(RuntimeException.class, () -> policy.evaluate(input));
        assertEquals("Maximum memory size exceeded", exception.getMessage());
    }

    @Test
    public void parsingInputExceedsMemory() {
        var policy =
                Opa.loadPolicy(
                        wasmFile,
                        OpaDefaultImports.builder().withMemoryInitial(3).withMemoryMax(4).build());
        var input = new String(new char[2 * 65536]).replace("\0", "a");
        var exception = assertThrows(OpaAbortException.class, () -> policy.input(input));
        Assertions.assertEquals("opa_abort - opa_malloc: failed", exception.getMessage());
    }

    @Test
    public void largeInputHostAndGuestGrowSuccessfully() {
        var policy =
                Opa.loadPolicy(
                        wasmFile,
                        OpaDefaultImports.builder().withMemoryInitial(2).withMemoryMax(8).build());
        var input = new String(new char[2 * 65536]).replace("\0", "a");
        assertDoesNotThrow(() -> policy.evaluate(input));
    }

    @Test
    public void doesNotLeakMemoryEvaluatingTheSamePolicyMultipleTimes() {
        var policy =
                Opa.loadPolicy(
                        wasmFile,
                        OpaDefaultImports.builder().withMemoryInitial(2).withMemoryMax(8).build());
        var input = new String(new char[2 * 65536]).replace("\0", "a");
        for (int i = 0; i < 16; i++) {
            assertDoesNotThrow(() -> policy.evaluate(input));
        }
    }
}
