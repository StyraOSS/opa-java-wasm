[![CI](https://github.com/andreaTP/opa-chicory/workflows/CI/badge.svg)](https://github.com/andreaTP/opa-chicory)
[![](https://jitpack.io/v/andreaTP/opa-chicory.svg)](https://jitpack.io/#andreaTP/opa-chicory)

> **This repo was recently transferred to the Styra organization and work is currently under way to get everything set up here**

# Open Policy Agent WebAssembly Java SDK (experimental)

This is an SDK for using WebAssembly (wasm) compiled [Open Policy Agent](https://www.openpolicyagent.org/) policies
with [Chicory](https://github.com/dylibso/chicory), a pure Java Wasm interpreter.

Initial implementation was based
on [Open Policy Agent WebAssemby NPM Module](https://github.com/open-policy-agent/npm-opa-wasm)
and [Open Policy Agent Ebassembly dotnet core SDK](https://github.com/me-viper/OpaDotNet)

## Why

We want fast in-process OPA policies evaluations, and avoid network bottlenecks when using [opa-java](https://github.com/StyraInc/opa-java).

# Getting Started

## Install the module

With Maven, add Jitpack to the `repositories` section:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

and add the core module dependency:

```xml
<dependency>
    <groupId>com.github.andreaTP.opa-chicory</groupId>
    <artifactId>opa-chicory-core</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```

## Usage

There are only a couple of steps required to start evaluating the policy.

### Import the module

```java
import com.github.andreaTP.opa.chicory.Opa;
```

### Load the policy

```java
var policy = Opa.loadPolicy(policyWasm);
```

The `policyWasm` ca be a variety of things, including raw byte array, `InputStream`, `Path`, `File`.
The content should be the compiled policy Wasm file, a valid WebAssembly module.

For example:

```java
var policy = Opa.loadPolicy(new File("policy.wasm"));
```

### Evaluate the Policy

The `OpaPolicy` object returned from `loadPolicy()` has a couple of important
APIs for policy evaluation:

`data(data)` -- Provide an external `data` document for policy evaluation.

- `data` MUST be a `String`, which assumed to be a well-formed stringified JSON

`evaluate(input)` -- Evaluates the policy using any loaded data and the supplied
`input` document.

- `input` parameter MUST be a `String` serialized `object`, `array` or primitive literal which assumed to be a well-formed stringified JSON

Example:

```java
input = '{"path": "/", "role": "admin"}';

var policy = Opa.loadPolicy(policyWasm);
var result = policy.evaluate(input);
System.out.println("Result is: " + result);
```

> For any `opa build` created WASM binaries the result set, when defined, will
> contain a `result` key with the value of the compiled entrypoint. See
> [https://www.openpolicyagent.org/docs/latest/wasm/](https://www.openpolicyagent.org/docs/latest/wasm/)
> for more details.

## Builtins support:

At the moment the following builtins are supported(and, by default, automatically injected when needed):

- Json
    - `json.is_valid`

- Yaml
    - `yaml.is_valid`
    - `yaml.marshal`
    - `yaml.unmarshal`

### Writing the policy

See
[https://www.openpolicyagent.org/docs/latest/how-do-i-write-policies/](https://www.openpolicyagent.org/docs/latest/how-do-i-write-policies/)

### Compiling the policy

Either use the
[Compile REST API](https://www.openpolicyagent.org/docs/latest/rest-api/#compile-api)
or `opa build` CLI tool.

For example:

```bash
opa build -t wasm -e example/allow example.rego
```

Which is compiling the `example.rego` policy file with the result set to
`data.example.allow`. The result will be an OPA bundle with the `policy.wasm`
binary included. See [./examples](./examples) for a more comprehensive example.

See `opa build --help` for more details.

## Development

To develop this library you need to have installed the following tools:

- Java 11+
- Maven
- the `opa` cli
- `tar`

the typical command to build and run the tests is:

```bash
mvn spotless:apply clean install
```

to disable the tests based on the Opa testsuite:

```bash
OPA_TESTSUITE=disabled mvn spotless:apply install
```
