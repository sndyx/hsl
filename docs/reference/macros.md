Docs / Reference / [Macros](macros.md)

---

# Macros

Macros are a set of processors that can alter your code in various ways.

## Define

The `#define` macro is used as follows:

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
 `$arg` (or `${arg}` when necessary)* to capture it in the value.

*It is necessary to use the extended `${arg}` syntax when the argument is
 followed by alphanumeric characters (eg: `$argms`, where you really expect `${arg}ms`).

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

`#define` macros must be located at the top of the file.

## Inline*

The `#inline` macro always preceeds a function and tells the compiler that the
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

*This macro is special in that inlined functions can still be used across files
 and projects.

## For

The `#for` macro is the compile-time equivalent of a `for` loop. It is used as
 follows:

```rust
#for (i in 1..5) {
    send_message("Count: $i")
}
```
```rust
// Expands to:
send_message("Count: 1")
send_message("Count: 2")
send_message("Count: 3")
send_message("Count: 4")
send_message("Count: 5")
```

Anything within the `#for` body will be repeated for every index of the range,
 and the current index can be retrieved with the given label (in this case,
 `i`). Like `#define`'s arguments, you can use `$i` (or `${i}` when 
 necessary) to capture it in the value.

## If

The `#if` macro is the compile-time equivalent of a `if` statement. It has the
 exact syntax that a regular `if` statement uses:

```rust
#if (100 + 10 == 110) {
    send_message("Hello!")
} #else {
    send_message("Goodbye!")
}
```

```rust
// Expands to:
send_message("Hello!")
```

The conditions within the `#if` macro must be known at compile-time, or the
 statement cannot be expanded.