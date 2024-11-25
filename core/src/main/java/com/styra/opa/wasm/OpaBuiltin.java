package com.styra.opa.wasm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class OpaBuiltin {
    private OpaBuiltin() {}

    public interface Builtin {
        String name();

        default int asBuiltin0(OpaWasm instance) {
            throw new RuntimeException("opa_builtin - " + name() + " doesn't implement builtin0");
        }

        default int asBuiltin1(OpaWasm instance, int _1) {
            throw new RuntimeException("opa_builtin - " + name() + " doesn't implement builtin1");
        }

        default int asBuiltin2(OpaWasm instance, int _1, int _2) {
            throw new RuntimeException("opa_builtin - " + name() + " doesn't implement builtin2");
        }

        default int asBuiltin3(OpaWasm instance, int _1, int _2, int _3) {
            throw new RuntimeException("opa_builtin - " + name() + " doesn't implement builtin3");
        }

        default int asBuiltin4(OpaWasm instance, int _1, int _2, int _3, int _4) {
            throw new RuntimeException("opa_builtin - " + name() + " doesn't implement builtin4");
        }
    }

    @FunctionalInterface
    public interface AsBuiltin0 {
        int apply(OpaWasm instance);
    }

    @FunctionalInterface
    public interface AsBuiltin1 {
        int apply(OpaWasm instance, int _1);
    }

    @FunctionalInterface
    public interface AsBuiltin2 {
        int apply(OpaWasm instance, int _1, int _2);
    }

    @FunctionalInterface
    public interface AsBuiltin3 {
        int apply(OpaWasm instance, int _1, int _2, int _3);
    }

    @FunctionalInterface
    public interface AsBuiltin4 {
        int apply(OpaWasm instance, int _1, int _2, int _3, int _4);
    }

    public static class Builtin0 implements Builtin {
        private final String name;
        private final AsBuiltin0 fn;

        Builtin0(String name, AsBuiltin0 fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int asBuiltin0(OpaWasm instance) {
            return fn.apply(instance);
        }
    }

    public static class Builtin1 implements Builtin {
        private final String name;
        private final AsBuiltin1 fn;

        public Builtin1(String name, AsBuiltin1 fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int asBuiltin1(OpaWasm instance, int _1) {
            return fn.apply(instance, _1);
        }
    }

    public static class Builtin2 implements Builtin {
        private final String name;
        private final AsBuiltin2 fn;

        public Builtin2(String name, AsBuiltin2 fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int asBuiltin2(OpaWasm instance, int _1, int _2) {
            return fn.apply(instance, _1, _2);
        }
    }

    public static class Builtin3 implements Builtin {
        private final String name;
        private final AsBuiltin3 fn;

        public Builtin3(String name, AsBuiltin3 fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int asBuiltin3(OpaWasm instance, int _1, int _2, int _3) {
            return fn.apply(instance, _1, _2, _3);
        }
    }

    public static class Builtin4 implements Builtin {
        private final String name;
        private final AsBuiltin4 fn;

        public Builtin4(String name, AsBuiltin4 fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int asBuiltin4(OpaWasm instance, int _1, int _2, int _3, int _4) {
            return fn.apply(instance, _1, _2, _3, _4);
        }
    }

    // helpers for a convenient API
    public static Builtin from(String name, Supplier<JsonNode> fn) {
        return new OpaBuiltin.Builtin0(
                name,
                (OpaWasm instance) -> {
                    try {
                        var result = Opa.jsonMapper.writeValueAsString(fn.get());
                        return instance.writeResult(result);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static Builtin from(String name, Function<JsonNode, JsonNode> fn) {
        return new OpaBuiltin.Builtin1(
                name,
                (OpaWasm instance, int strAddr) -> {
                    try {
                        var inputStr = Opa.jsonMapper.readTree(instance.readString(strAddr));
                        var result = Opa.jsonMapper.writeValueAsString(fn.apply(inputStr));
                        return instance.writeResult(result);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static Builtin from(String name, BiFunction<JsonNode, JsonNode, JsonNode> fn) {
        return new OpaBuiltin.Builtin2(
                name,
                (OpaWasm instance, int strAddr1, int strAddr2) -> {
                    try {
                        var inputStr1 = Opa.jsonMapper.readTree(instance.readString(strAddr1));
                        var inputStr2 = Opa.jsonMapper.readTree(instance.readString(strAddr2));
                        var result =
                                Opa.jsonMapper.writeValueAsString(fn.apply(inputStr1, inputStr2));
                        return instance.writeResult(result);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @FunctionalInterface
    public interface ThreeFunction {
        JsonNode apply(JsonNode arg0, JsonNode arg1, JsonNode arg2);
    }

    public static Builtin from(String name, ThreeFunction fn) {
        return new OpaBuiltin.Builtin3(
                name,
                (OpaWasm instance, int strAddr1, int strAddr2, int strAddr3) -> {
                    try {
                        var inputStr1 = Opa.jsonMapper.readTree(instance.readString(strAddr1));
                        var inputStr2 = Opa.jsonMapper.readTree(instance.readString(strAddr2));
                        var inputStr3 = Opa.jsonMapper.readTree(instance.readString(strAddr3));
                        var result =
                                Opa.jsonMapper.writeValueAsString(
                                        fn.apply(inputStr1, inputStr2, inputStr3));
                        return instance.writeResult(result);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @FunctionalInterface
    public interface FourFunction {
        JsonNode apply(JsonNode arg0, JsonNode arg1, JsonNode arg2, JsonNode arg3);
    }

    public static Builtin from(String name, FourFunction fn) {
        return new OpaBuiltin.Builtin4(
                name,
                (OpaWasm instance, int strAddr1, int strAddr2, int strAddr3, int strAddr4) -> {
                    try {
                        var inputStr1 = Opa.jsonMapper.readTree(instance.readString(strAddr1));
                        var inputStr2 = Opa.jsonMapper.readTree(instance.readString(strAddr2));
                        var inputStr3 = Opa.jsonMapper.readTree(instance.readString(strAddr3));
                        var inputStr4 = Opa.jsonMapper.readTree(instance.readString(strAddr4));
                        var result =
                                Opa.jsonMapper.writeValueAsString(
                                        fn.apply(inputStr1, inputStr2, inputStr3, inputStr4));
                        return instance.writeResult(result);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
