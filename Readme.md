[![CI](https://github.com/StyraInc/opa-java-wasm/workflows/CI/badge.svg)](https://github.com/StyraInc/opa-java-wasm)
[![GitHub Release](https://img.shields.io/github/tag/StyraInc/opa-java-wasm.svg?style=flat&color=green)](https://github.com/StyraInc/opa-java-wasm/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.styra.opa/opa-java-wasm/badge.svg?style=flat&color=green)](https://central.sonatype.com/artifact/com.styra.opa/opa-java-wasm)

# Open Policy Agent WebAssembly Java SDK (experimental)

This is an SDK for using WebAssembly(wasm) compiled [Open Policy Agent](https://www.openpolicyagent.org/) policies
with Java powered by [Chicory](https://github.com/dylibso/chicory), a pure Java Wasm interpreter.

Initial implementation was based
on [Open Policy Agent WebAssemby NPM Module](https://github.com/open-policy-agent/npm-opa-wasm)
and [Open Policy Agent WebAssembly dotnet core SDK](https://github.com/me-viper/OpaDotNet)

## Why

We want **fast**, **in-process** and **secure** OPA policies evaluation, and avoid network bottlenecks when using [opa-java](https://github.com/StyraInc/opa-java).

# Getting Started

## Install the module

With Maven add the core module dependency:

```xml
<dependency>
    <groupId>com.styra.opa</groupId>
    <artifactId>opa-java-wasm</artifactId>
    <version>latest_release</version>
</dependency>
```

## Usage

There are only a couple of steps required to start evaluating the policy.

### Import the module

```java
import com.styra.opa.wasm.Opa;
```

### Load the policy

```java
var policy = OpaPolicy.builder().withPolicy(policyWasm).build();
```

The `policyWasm` ca be a variety of things, including raw byte array, `InputStream`, `Path`, `File`.
The content should be the compiled policy Wasm file, a valid WebAssembly module.

For example:

```java
var policy = OpaPolicy.builder().withPolicy(new File("policy.wasm")).build();
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

var policy = OpaPolicy.builder().withPolicy(policyWasm).build();
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

## Support

This SDK is community supported and maintained and is not under the umbrella of SDKs eligible for Enterprise support from Styra. For bug reports and feature requests, please use Github issues. For real-time support, please join the Open Policy Agent or Styra Community slack organizations.

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
