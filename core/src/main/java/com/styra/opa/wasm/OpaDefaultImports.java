package com.styra.opa.wasm;

import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.styra.opa.wasm.builtins.Provided;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// to be implemented by the user
public class OpaDefaultImports implements OpaImports {
    // TODO: review the default min, max limits
    private static final int DEFAULT_MEMORY_INITIAL = 10;
    private static final int DEFAULT_MEMORY_MAX = MemoryLimits.MAX_PAGES;
    protected final Memory memory;
    protected OpaBuiltin.Builtin[] builtins = new OpaBuiltin.Builtin[0];
    protected boolean defaultBuiltins = true;

    private OpaDefaultImports(
            int initial, int maximum, boolean defaultBuiltins, OpaBuiltin.Builtin[] builtins) {
        this.builtins = builtins;
        this.defaultBuiltins = defaultBuiltins;
        this.memory = new Memory(new MemoryLimits(initial, maximum));
    }

    @Override
    public void initializeBuiltins(Map<String, Integer> mappings) {
        var result = new OpaBuiltin.Builtin[mappings.size()];
        // Default initialization to have proper error messages
        for (var m : mappings.entrySet()) {
            result[m.getValue()] = () -> m.getKey();
        }
        for (var builtin : this.builtins) {
            if (mappings.containsKey(builtin.name())) {
                result[mappings.get(builtin.name())] = builtin;
            }
        }
        if (defaultBuiltins) {
            // provided builtins override anything with clashing name
            var all = Provided.all();
            for (var builtin : all) {
                if (mappings.containsKey(builtin.name())) {
                    result[mappings.get(builtin.name())] = builtin;
                }
            }
        }
        this.builtins = result;
    }

    public Memory memory() {
        return memory;
    }

    @Override
    public void opaPrintln(OpaWasm wasm, int ptr) {
        var message = wasm.memory().readCString(ptr);
        System.out.println("opa_println - " + message);
    }

    @Override
    public void opaAbort(OpaWasm instance, int ptr) {
        var errorMessage = instance.memory().readCString(ptr);
        throw new OpaAbortException("opa_abort - " + errorMessage);
    }

    @Override
    public int opaBuiltin0(OpaWasm instance, int builtinId, int ctx) {
        return builtins[builtinId].asBuiltin0(instance);
    }

    @Override
    public int opaBuiltin1(OpaWasm instance, int builtinId, int ctx, int _1) {
        return builtins[builtinId].asBuiltin1(instance, _1);
    }

    @Override
    public int opaBuiltin2(OpaWasm instance, int builtinId, int ctx, int _1, int _2) {
        return builtins[builtinId].asBuiltin2(instance, _1, _2);
    }

    @Override
    public int opaBuiltin3(OpaWasm instance, int builtinId, int ctx, int _1, int _2, int _3) {
        return builtins[builtinId].asBuiltin3(instance, _1, _2, _3);
    }

    @Override
    public int opaBuiltin4(
            OpaWasm instance, int builtinId, int ctx, int _1, int _2, int _3, int _4) {
        return builtins[builtinId].asBuiltin4(instance, _1, _2, _3, _4);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {}

        private int memory_initial = DEFAULT_MEMORY_INITIAL;
        private int memory_max = DEFAULT_MEMORY_MAX;
        private boolean defaultBuiltins = true;
        private List<OpaBuiltin.Builtin> builtins = new ArrayList<>();

        public Builder withMemoryInitial(int initial) {
            this.memory_initial = initial;
            return this;
        }

        public Builder withMemoryMax(int max) {
            this.memory_max = max;
            return this;
        }

        public Builder withDefaultBuiltins(boolean defaultBuiltins) {
            this.defaultBuiltins = defaultBuiltins;
            return this;
        }

        public Builder addBuiltins(OpaBuiltin.Builtin... builtins) {
            for (var builtin : builtins) {
                this.builtins.add(builtin);
            }
            return this;
        }

        public OpaDefaultImports build() {
            return new OpaDefaultImports(
                    memory_initial,
                    memory_max,
                    defaultBuiltins,
                    builtins.toArray(OpaBuiltin.Builtin[]::new));
        }
    }
}
