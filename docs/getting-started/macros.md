# Macros

Macros are a set of processors that can alter your code in various ways.

## Define

The `define` macro is declared as follows:

```rust
#define NAME value
```

Using this macro, we can insert `value` wherever `NAME` is used.

```rust
#define HELLO_WORLD "Hello, world!"

fn test() {
    send_message(HELLO_WORLD)
    // Sends "Hello, world!"
}
```

To pass arguments into a `define` statement, use parenthesis `(args...)`, and use
 `$arg` (or `${arg}` when necessary) to capture it in the value.

```rust
#define TWICE(stmt) $stmt $stmt

fn test() {
    TWICE(x *= 5)
}
```

```rust
// Expands to:
fn test() {
    x *= 5
    x *= 5
}
```

`define` macros must be located at the top of the file.

## Inline

The `inline` macro always preceeds a function and tells the compiler that the
 contents of the following function should be inserted into its callsites and
 removed.

```rust
fn test() {
  x = multiply(10, 20)
}

#inline
fn multiply(_x, _y) {
  return _x * _y
}
```

```rust
// Expands to:
fn test() {
  x = 10 * 20
}
```
