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

Team variables are declared `team.stat`, where `team` is the team name
 and `stat` is the stat name.
```rust
blue.kills = 0
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

## Conditionals

```rust
if (a > b) {
    send_message("a")
} else {
    send_message("b")
}
```

In HSL, `if` can also be used as an expression.

```rust
x = if (a > b) a else b
```

## Built-in Functions

Built-in functions provide direct mappings to most actions. They
 are syntactically identical to normal functions.

```rust
send_message("Hello from HSL!")
kill()
send_to_lobby("UHC Champions")
```

You can get a detailed list of all built-in functions
[here](../reference/built-ins.md).

---

Next: [Advanced Syntax](advanced-syntax.md)