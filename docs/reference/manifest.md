Docs / Reference / [Manifest](manifest.md)

---

# Manifest

## Project

```toml
[project]

# Required properties
name = "example"
version = "1.0.0"

# Optional properties

mode = "strict"
target = "housing+"
```

## Dependencies

Dependencies are declared as follows:

```toml
[dependencies]
test = "github.com/sndyx/hsl-test" # External link
```

To specify a version, use the following syntax:

```toml
[dependencies]
test = { git = "github.com/sndyx/hsl-test", version = "2.0.0" }
```

The key must match the name of the project you are using as a dependency.