package com.styra.opa.wasm;

import com.dylibso.chicory.runtime.Memory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// final user API
// directly porting:
// https://github.com/open-policy-agent/npm-opa-wasm/blob/main/README.md
public class OpaPolicy {
    private final OpaWasm wasm;
    private int baseHeapPtr = -1;
    private int dataHeapPtr = -1;
    private int dataAddr = -1;
    private int inputAddr = -1;
    private int resultAddr = -1;
    private int entrypoint;

    private OpaPolicy(OpaWasm wasm) {
        this.wasm = wasm;

        if (!(wasm.opaWasmAbiVersion() == 1 && wasm.opaWasmAbiMinorVersion() == 3)) {
            throw new IllegalArgumentException(
                    "Invalid version, supported 1.3, detected "
                            + wasm.opaWasmAbiVersion()
                            + "."
                            + wasm.opaWasmAbiMinorVersion());
        }

        this.baseHeapPtr = wasm.opaHeapPtrGet();
        this.dataHeapPtr = this.baseHeapPtr;
        this.dataAddr = -1;
        wasm.opaHeapPtrSet(this.dataHeapPtr);

        // map the builtins
        try {
            var mappings = new HashMap<String, Integer>();
            int builtinsAddr = wasm.builtins();
            var builtinsStrAddr = wasm.opaJsonDump(builtinsAddr);
            var builtinsStr = wasm.memory().readCString(builtinsStrAddr);
            wasm.opaFree(builtinsStrAddr);
            wasm.opaFree(builtinsAddr);
            var fields = wasm.jsonMapper().readTree(builtinsStr).fields();
            while (fields.hasNext()) {
                var field = fields.next();
                mappings.put(field.getKey(), field.getValue().intValue());
            }
            wasm.imports().initializeBuiltins(mappings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public OpaPolicy entrypoint(int entrypoint) {
        this.entrypoint = entrypoint;
        return this;
    }

    public OpaPolicy entrypoint(String entrypoint) {
        this.entrypoint = findEntrypoint(entrypoint);
        return this;
    }

    private int loadJson(String data) {
        var dataStrAddr = wasm.opaMalloc(data.length());
        wasm.memory().writeCString(dataStrAddr, data);
        var dstAddr = wasm.opaJsonParse(dataStrAddr, data.length());
        wasm.opaFree(dataStrAddr);
        return dstAddr;
    }

    private String dumpJson(int addr) {
        int resultStrAddr = wasm.opaJsonDump(addr);
        var result = wasm.memory().readCString(resultStrAddr);
        wasm.opaFree(resultStrAddr);
        return result;
    }

    // data MUST be a serializable object or ArrayBuffer, which assumed to be a well-formed
    // stringified JSON
    public OpaPolicy data(String data) {
        wasm.opaHeapPtrSet(this.baseHeapPtr);
        this.dataAddr = loadJson(data);
        this.dataHeapPtr = wasm.opaHeapPtrGet();
        return this;
    }

    public OpaPolicy data(JsonNode data) {
        try {
            return data(wasm.jsonMapper().writeValueAsString(data));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize the provided data to Json: " + data, e);
        }
    }

    public OpaPolicy input(String input) {
        var inputLen = input.getBytes().length;
        var delta = this.dataHeapPtr + inputLen - (wasm.memory().pages() * Memory.PAGE_SIZE);
        if (delta > 0) {
            // TODO: similar logic might go into Chicory itself?
            var pageSize = (int) Math.ceil(delta / Memory.PAGE_SIZE);
            var grown = wasm.memory().grow(pageSize);
            if (grown == -1) {
                throw new RuntimeException("Maximum memory size exceeded");
            }
        }

        this.inputAddr = loadJson(input);
        return this;
    }

    public Map<String, Integer> entrypoints() {
        try {
            var json = dumpJson(wasm.entrypoints());
            var entrypoints =
                    wasm.jsonMapper()
                            .readValue(json, new TypeReference<HashMap<String, Integer>>() {});
            return entrypoints;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse the response from \"entrypoints()\"", e);
        }
    }

    public int findEntrypoint(String name) {
        var entrypoints = entrypoints();
        if (!entrypoints.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Entrypoint " + name + " is not defined in this policy");
        }
        return entrypoints.get(name);
    }

    public String evaluate() {
        var ctxAddr = wasm.opaEvalCtxNew();
        if (this.dataAddr == -1) {
            data("");
        }
        wasm.opaEvalCtxSetData(ctxAddr, this.dataAddr);
        if (this.inputAddr == -1) {
            input("");
        }
        wasm.opaEvalCtxSetInput(ctxAddr, this.inputAddr);
        wasm.opaEvalCtxSetEntrypoint(ctxAddr, this.entrypoint);

        var evalResult = wasm.eval(ctxAddr);
        if (evalResult != OpaErrorCode.OPA_ERR_OK) {
            throw new RuntimeException(
                    "Error evaluating the Opa Policy, returned code is: " + evalResult);
        }

        this.resultAddr = wasm.opaEvalCtxGetResult(ctxAddr);
        var result = dumpJson(resultAddr);
        return result;
    }

    public String evaluate(String input) {
        input(input);
        return evaluate();
    }

    public String evaluate(JsonNode input) {
        try {
            input(wasm.jsonMapper().writeValueAsString(input));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return evaluate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OpaImports imports;
        private InputStream is;
        private ObjectMapper jsonMapper;
        private ObjectMapper yamlMapper;

        private Builder() {}
        ;

        public Builder withImports(OpaImports imports) {
            this.imports = imports;
            return this;
        }

        public Builder withPolicy(InputStream is) {
            this.is = is;
            return this;
        }

        public Builder withPolicy(ByteBuffer buffer) {
            return withPolicy(buffer.array());
        }

        public Builder withPolicy(byte[] buffer) {
            return withPolicy(new ByteArrayInputStream(buffer));
        }

        public Builder withPolicy(Path path) {
            return withPolicy(path.toFile());
        }

        public Builder withPolicy(File file) {
            try {
                return withPolicy(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("File not found at path: " + file.getPath(), e);
            }
        }

        public Builder withJsonMapper(ObjectMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder withYamlMapper(ObjectMapper yamlMapper) {
            this.yamlMapper = yamlMapper;
            return this;
        }

        public OpaPolicy build() {
            // Default management
            if (jsonMapper == null) {
                jsonMapper = DefaultMappers.jsonMapper;
            }
            if (yamlMapper == null) {
                yamlMapper = DefaultMappers.yamlMapper;
            }
            if (imports == null) {
                imports = OpaDefaultImports.builder().build();
            }
            Objects.requireNonNull(is);

            return new OpaPolicy(
                    OpaWasm.builder()
                            .withImports(imports)
                            .withInputStream(is)
                            .withJsonMapper(jsonMapper)
                            .withYamlMapper(yamlMapper)
                            .build());
        }
    }
}
