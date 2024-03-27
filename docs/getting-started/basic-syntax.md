Docs / Getting Started / [Basic Syntax](basic-syntax.md)

---

# Basic Syntax

This is a collection of basic syntax elements with examples. You can find
 more examples [here](https://github.com/sndyx/hsl/tree/master/examples).

You can also get a detailed list of all built-in functions
 [here](../reference/built-ins.md).

## Comments

HSL supports single-line (or **end-of-line**) and multi-line (**block**)
 comments.

```rust
// This is an end-of-line comment

/* This is a block comment
   on multiple lines. */
```

## Functions

A basic function.

```rust
fn test() {
    send_message("Hello world!")
}
```

## Variables

Player variables can be declared by simply assigning their name to a
 value.

```rust
x = 5
```

Global variables can be declared by prefixing a variable name with `@`.

```rust
@kills = 5
```

Variables can be modified using various binary operators.

```rust
x += 1 // Increment
x -= 1 // Decrement
x *= y // Multiply
x /= y // Divide
```

Variable declarations must be within a function.

```rust
x = 5 // Will not compile!

fn test() {
    x = 5 // Will compile
}
```

---

Next: [Advanced Syntax](advanced-syntax.md)